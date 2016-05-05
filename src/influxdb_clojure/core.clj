(ns influxdb-clojure.core
  (:require [clojure.walk :as walk]
            [clojure.string :refer [upper-case]]
            [clj-time.core :as t])
  (:import (org.influxdb InfluxDB InfluxDBFactory InfluxDB$ConsistencyLevel)
           (org.influxdb.dto BatchPoints Point BatchPoints$Builder Point$Builder Query QueryResult QueryResult$Result QueryResult$Series)
           (java.util.concurrent TimeUnit)))

(def ^:private default-uri "http://localhost:8086")

(def ^:private default-user "root")

(def ^:private default-password "root")

(defn connect
  "Connects to the given InfluxDB endpoint and returns a connection"
  (^InfluxDB []
   (connect default-uri))
  (^InfluxDB [uri]
   (connect uri default-user default-password))
  (^InfluxDB [uri user password]
   (InfluxDBFactory/connect uri user password)))

(defn create-database
  "Creates the database with the given name"
  [^InfluxDB conn ^String database-name]
  (.createDatabase conn database-name))

(defn delete-database
  "Deletes the database with the given name"
  [^InfluxDB conn ^String database-name]
  (.deleteDatabase conn database-name))

(defn databases
  "Lists existing databases"
  [^InfluxDB conn]
  (sort (.describeDatabases conn)))

(defn ping
  "Pings the InfluxDB instance. Returns InfluxDB version and response time."
  [^InfluxDB conn]
  (let [pong (.ping conn)]
    {:version (.getVersion pong) :response-time (.getResponseTime pong)}))

(defn- convert-point [batch-time batch-tags point]
  (let [{:keys [measurement tags fields time]
         :or   {time batch-time}} point
        ^Point$Builder point-builder (Point/measurement measurement)]
    (doto point-builder
      (.time time TimeUnit/MILLISECONDS)
      (.fields (walk/stringify-keys fields))
      (.tag (walk/stringify-keys (merge batch-tags tags))))
    (.build point-builder)))

(defn write-points
  "Writes points to the database"
  ([^InfluxDB conn ^String database-name points]
   (write-points conn database-name points {}))
  ([^InfluxDB conn ^String database-name points opts]
   (let [{:keys [tags consistency retention-policy]
          :or   {tags             {}
                 consistency      :any
                 retention-policy "default"}} opts
         ^BatchPoints$Builder batch-builder (BatchPoints/database database-name)
         batch-time (System/currentTimeMillis)
         point-objects (map (partial convert-point batch-time tags) points)]
     (doto batch-builder
       (.retentionPolicy retention-policy)
       (.consistency (InfluxDB$ConsistencyLevel/valueOf
                       (upper-case (if (keyword? consistency)
                                     (name consistency)
                                     consistency)))))
     (doseq [point-object point-objects]
       (.point batch-builder point-object))
     (.write conn (.build batch-builder)))))
