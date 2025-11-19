(defproject com.rpl/lof-research-lab "1.0.0-SNAPSHOT"
 :dependencies [[com.rpl/rama-helpers "0.10.0"]
                [org.clojure/clojure "1.12.0"]]
 :repositories [["releases" {:id "maven-releases"
                             :url "https://nexus.redplanetlabs.com/repository/maven-public-releases"}]]

 :profiles {:dev {:resource-paths ["src/test/clj/"]}
            :provided {:dependencies [[com.rpl/rama "1.2.0"]]}})
  
