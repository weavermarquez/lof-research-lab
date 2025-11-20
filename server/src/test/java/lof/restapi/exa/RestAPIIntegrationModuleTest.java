package lof.restapi.exa;

import org.junit.Test;
import static org.junit.Assert.*;

import com.rpl.rama.*;
import com.rpl.rama.test.*;
import lof.restapi.exa.data.SearchRequest;

public class RestAPIIntegrationModuleTest {
  @Test
  public void test() throws Exception {
    // InProcessCluster simulates a full Rama cluster in-process and is an ideal environment for experimentation and
    // unit-testing.
    try(InProcessCluster ipc = InProcessCluster.create()) {
      RestAPIIntegrationModule module = new RestAPIIntegrationModule();
      // By default a module's name is the same as its class name.
      String moduleName = module.getClass().getName();
      ipc.launchModule(module, new LaunchConfig(4, 2));

      // Client usage of IPC is identical to using a real cluster. Depot and PState clients are fetched by
      // referencing the module name along with the variable used to identify the depot/PState within the module.
      Depot getDepot = ipc.clusterDepot(moduleName, "*getDepot");
      PState responses = ipc.clusterPState(moduleName, "$$responses");

      String url = "https://official-joke-api.appspot.com/random_joke";

      // This checks the behavior of the module by appending a few URLs and printing the responses recorded in the
      // PState. To write a real test with actual assertions, it's best to test the behavior of the module with
      // the external REST API calls mocked out by using some sort of dependency injection.
      getDepot.append(url);
      System.out.println("Response 1: " + responses.selectOne(Path.key(url)));
      getDepot.append(url);
      System.out.println("Response 2: " + responses.selectOne(Path.key(url)));
    }
  }

  @Test
  public void testExaSearchPost() throws Exception {
    try(InProcessCluster ipc = InProcessCluster.create()) {
      RestAPIIntegrationModule module = new RestAPIIntegrationModule();
      String moduleName = module.getClass().getName();
      ipc.launchModule(module, new LaunchConfig(4, 2));

      Depot postDepot = ipc.clusterDepot(moduleName, "*postDepot");
      PState responses = ipc.clusterPState(moduleName, "$$responses");

      String apiKey = System.getenv("EXA_API_KEY");
      if(apiKey == null || apiKey.isEmpty()) {
        throw new IllegalStateException("EXA_API_KEY must be set to run Exa integration test");
      }

      String query = "Laws of Form cybernetics overview";
      SearchRequest req = new SearchRequest(query);

      postDepot.append(req);

      String responseBody = null;
      for(int i = 0; i < 30; i++) {
        responseBody = (String) responses.selectOne(Path.key(query));
        if(responseBody != null) break;
        Thread.sleep(500);
      }

      assertNotNull("Did not receive search response from Exa", responseBody);
      assertTrue("Response should include results array", responseBody.contains("\"results\""));
      System.out.println("Search response: " + responseBody);
    }
  }
}
