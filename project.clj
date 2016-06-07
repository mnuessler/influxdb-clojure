(defproject influxdb/influxdb-clojure "0.3.0-SNAPSHOT"
  :description "Simple InfluxDB client for Clojure, implemented as a wrapper
                around the InfluxDB Java client. Compatible with InfluxDB >= 0.9."
  :url "https://github.com/mnuessler/influxdb-clojure"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :min-lein-version "2.5.0"
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.influxdb/influxdb-java "2.2"]
                 [org.clojure/tools.logging "0.3.1"]
                 [clj-time "0.12.0"]]
  :source-paths ["src/main/clojure"]
  :test-paths ["src/test/clojure"]
  :resource-paths ["src/main/resources"]
  :target-path "target/%s"
  :profiles {:dev {:dependencies [[version-clj "0.1.2"]
                                  [org.clojure/tools.logging "0.3.1"]
                                  [org.slf4j/slf4j-api "1.7.21"]
                                  [org.slf4j/jcl-over-slf4j "1.7.21"]
                                  [org.apache.logging.log4j/log4j-core "2.6"]
                                  [org.apache.logging.log4j/log4j-slf4j-impl "2.6"]]
                   :resource-paths ["src/test/resources" "src/main/resources"]}})
