(defproject influxdb/influxdb-clojure "0.1.0"
  :description "Minimalistic InfluxDB client for Clojure, implemented as a wrapper
                around the InfluxDB Java client. Compatible with InfluxDB >= 0.9."
  :url "https://github.com/mnuessler/influxdb-clojure"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :min-lein-version "2.5.0"
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.influxdb/influxdb-java "2.2"]
                 [org.clojure/tools.logging "0.3.1"]
                 [clj-time "0.11.0"]]
  :target-path "target/%s")
