(ns lof.lab.record-seeder-module
  (:use [com.rpl.rama]
        [com.rpl.rama.path])
  (:require [com.rpl.rama.ops :as ops]
            [lof.lab.data.record :as rec])
  (:import [lof.research.lab.parsers ZoteroXmlParser]
           [lof.lab.data.record Record]))

;; This module demonstrates XML ingestion -> PState seeding for academic papers.
;; It takes parsed Record objects from a Zotero XML export and stores them in a
;; PState indexed by canonical-id.

(defmodule RecordSeederModule
  [setup topologies]
  ;; Depot for Record ingestion, partitioned by canonical-id
  (declare-depot setup *records-depot (hash-by :canonical-id))

  ;; Microbatch topology for ETL processing
  (let [mb (microbatch-topology topologies "record-seeder")]
    ;; PState: Map from canonical-id (String) -> Record
    (declare-pstate mb $$records-by-id {String Record})

    ;; Dataflow: depot -> explode batch -> partition by ID -> store in PState
    (<<sources mb
      ;; Subscribe to depot, bind microbatch
      (source> *records-depot :> %microbatch)

      ;; Explode batch and destructure Record fields
      (%microbatch :> {:keys [*canonical-id] :as *record})

      ;; Partition by canonical-id
      (|hash *canonical-id)

      ;; Write to PState
      (local-transform> [(keypath *canonical-id) (termval *record)]
                        $$records-by-id))))

(defn -main
  "Parse XML, seed depot, and query PState"
  [& args]
  (println "=== Record Seeder V0 (Clojure) ===")
  (println "Parsing XML and seeding PState with first 10 records\n")

  ;; Parse XML file using Java interop (first 10 records)
  (let [xml-path "../gsbbib__pretty.xml"
        java-records (ZoteroXmlParser/parseXmlFile xml-path 10)
        ;; Convert Java Records to Clojure Records
        records (mapv (fn [jr]
                        (rec/->Record
                          (.canonicalId jr)
                          (vec (.authors jr))
                          (.title jr)
                          (.year jr)
                          (.doi jr)
                          (.url jr)))
                      java-records)]

    (if (empty? records)
      (println "No records parsed. Exiting.")

      ;; Launch in-process Rama cluster
      (with-open [ipc (com.rpl.rama.test.InProcessCluster/create)]
        (com.rpl.rama.test.InProcessCluster/launchModule
          ipc RecordSeederModule
          (com.rpl.rama.test.LaunchConfig. 4 4))

        (let [module-name (get-module-name RecordSeederModule)
              records-depot (foreign-depot ipc module-name "*records-depot")
              records-by-id (foreign-pstate ipc module-name "$$records-by-id")]

          ;; Append all parsed records to depot
          (println (str "Appending " (count records) " records to depot..."))
          (doseq [record records]
            (foreign-append! records-depot record))

          ;; Wait for all records to be processed
          (com.rpl.rama.test.InProcessCluster/waitForMicrobatchProcessedCount
            ipc module-name "record-seeder" (count records))
          (println "All records processed!\n")

          ;; Query PState and display results
          (println "=== Sample Records from PState ===\n")
          (doseq [i (range (min 3 (count records)))]
            (let [record (nth records i)
                  id (:canonical-id record)
                  retrieved (foreign-select-one (keypath id) records-by-id)]

              (if retrieved
                (do
                  (println (str "Record " (inc i) ":"))
                  (println (str "  ID: " (:canonical-id retrieved)))
                  (println (str "  Title: " (:title retrieved)))
                  (println (str "  Authors: " (:authors retrieved)))
                  (println (str "  Year: " (:year retrieved)))
                  (println (str "  DOI: " (or (:doi retrieved) "N/A")))
                  (println (str "  URL: " (or (:url retrieved) "N/A")))
                  (println))
                (println (str "ERROR: Could not retrieve record with ID: " id)))))

          (println "=== Summary ===")
          (println (str "Total records in PState: " (count records)))
          (println "V0 seeder complete!"))))))

(comment
  ;; REPL testing
  (-main)

  ;; Or manual step-by-step:
  (require '[com.rpl.rama.test :as rtest])

  (def ipc (rtest/create-ipc))
  (rtest/launch-module! ipc RecordSeederModule {:tasks 4 :threads 2})

  (def module-name (get-module-name RecordSeederModule))
  (def depot (foreign-depot ipc module-name "*records-depot"))
  (def pstate (foreign-pstate ipc module-name "$$records-by-id"))

  ;; Append a test record
  (foreign-append! depot
                   (rec/make-record {:authors ["Test, Author"]
                                     :title "Test Title"
                                     :year 2024
                                     :doi nil
                                     :url nil}))

  ;; Query
  (foreign-select-one (keypath "test-2024-test-title") pstate)

  (.close ipc)
  )
