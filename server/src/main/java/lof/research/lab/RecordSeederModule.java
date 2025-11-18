package lof.research.lab;

import com.rpl.rama.*;
import com.rpl.rama.module.*;
import com.rpl.rama.ops.Ops;
import com.rpl.rama.test.*;
import lof.research.lab.data.Record;
import lof.research.lab.parsers.ZoteroXmlParser;

import java.util.List;

public class RecordSeederModule implements RamaModule {

  @Override
  public void define(Setup setup, Topologies topologies) {
    // Declare depot for Record ingestion
    setup.declareDepot("*recordsDepot", Depot.random());

    // Create microbatch topology for ETL processing
    MicrobatchTopology mb = topologies.microbatch("recordSeeder");

    // Declare PState: Map from canonicalId -> Record
    mb.pstate("$$recordsById", PState.mapSchema(String.class, Record.class));

    // Data flow: depot -> explode -> partition by ID -> store in PState
    mb.source("*recordsDepot").out("*microbatch")
      .explodeMicrobatch("*microbatch").out("*record")
      .each((Record r) -> r.canonicalId, "*record").out("*id")
      .hashPartition("*id")
      .localTransform("$$recordsById", Path.key("*id").termVal("*record"));
  }

  public static void main(String[] args) throws Exception {
    System.out.println("=== Record Seeder V0 ===");
    System.out.println("Parsing XML and seeding PState with first 10 records\n");

    // Parse XML file (first 10 records)
    String xmlPath = "../gsbbib__pretty.xml";
    List<Record> records = ZoteroXmlParser.parseXmlFile(xmlPath, 10);

    if (records.isEmpty()) {
      System.err.println("No records parsed. Exiting.");
      return;
    }

    // Launch in-process Rama cluster
    try (InProcessCluster cluster = InProcessCluster.create()) {
      RamaModule module = new RecordSeederModule();
      cluster.launchModule(module, new LaunchConfig(4, 4));
      String moduleName = module.getClass().getName();

      // Get depot and PState references
      Depot recordsDepot = cluster.clusterDepot(moduleName, "*recordsDepot");
      PState recordsById = cluster.clusterPState(moduleName, "$$recordsById");

      // Append all parsed records to depot
      System.out.println("Appending " + records.size() + " records to depot...");
      for (Record record : records) {
        recordsDepot.append(record);
      }

      // Wait for all records to be processed
      cluster.waitForMicrobatchProcessedCount(moduleName, "recordSeeder", records.size());
      System.out.println("All records processed!\n");

      // Query PState and display results
      System.out.println("=== Sample Records from PState ===\n");
      for (int i = 0; i < Math.min(3, records.size()); i++) {
        Record record = records.get(i);
        String id = record.canonicalId;
        Record retrieved = (Record) recordsById.selectOne(Path.key(id));

        if (retrieved != null) {
          System.out.println("Record " + (i + 1) + ":");
          System.out.println("  ID: " + retrieved.canonicalId);
          System.out.println("  Title: " + retrieved.title);
          System.out.println("  Authors: " + retrieved.authors);
          System.out.println("  Year: " + retrieved.year);
          System.out.println("  DOI: " + (retrieved.doi != null ? retrieved.doi : "N/A"));
          System.out.println("  URL: " + (retrieved.url != null ? retrieved.url : "N/A"));
          System.out.println();
        } else {
          System.err.println("ERROR: Could not retrieve record with ID: " + id);
        }
      }

      System.out.println("=== Summary ===");
      System.out.println("Total records in PState: " + records.size());
      System.out.println("V0 seeder complete!");
    }
  }
}
