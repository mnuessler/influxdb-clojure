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

(def ^:private connection-default-opts
  {:connect-timeout (* 1 1000)
   :read-timeout    (* 5 1000)
   :write-timeout   (* 5 1000)})

(defn connect
  "Connects to the given InfluxDB endpoint and returns a connection"
  (^InfluxDB []
   (connect default-uri))
  (^InfluxDB [uri]
   (connect uri default-user default-password))
  (^InfluxDB [uri user password]
   (connect uri user password {}))
  (^InfluxDB [uri user password {:keys [client]}]
   (if-not client
     (InfluxDBFactory/connect uri user password)
     (InfluxDBFactory/connect uri user password client))))

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
    {:version       (.getVersion pong)
     :response-time (.getResponseTime pong)}))

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
                 retention-policy "autogen"}} opts
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

(defn convert-query-result [^QueryResult query-result]
  (letfn [(convert-series [^QueryResult$Series series]
            {:name   (.getName series)
             :colums (into [] (.getColumns series))
             :values (into [] (map #(into [] %) (.getValues series)))})
          (convert-result [^QueryResult$Result result]
            (if (.hasError result)
              {:error (.getError result)})
            {:series (into [] (map convert-series (.getSeries result)))})]
    (let [response {}]
      (if (.hasError query-result)
        (assoc response :error (.getError query-result))
        (->> query-result
             .getResults
             (map convert-result)
             (into [])
             (assoc response :results))))))

(defn query
  "Executes a database query"
  ([^InfluxDB conn ^String query-str]
   (query conn query-str nil))
  ([^InfluxDB conn ^String query-str ^String database-name]
   (let [^Query query (Query. query-str database-name)
         ^QueryResult query-result (.query conn query)]
     (convert-query-result query-result))))

(defn query-chunked
  "Executes a chunked database query executing callback every chunk
  after last chunk has been processed, sends a last chunk in the
  form of {:error :done}"
  ([^InfluxDB conn ^String query-str ^Integer chunk-size ^String database-name callback-fn]
   (let [^Query query (Query. query-str database-name)
         rewrite-done #(if (= (:error %) "DONE") {:error :done} %)
         consumer (reify java.util.function.Consumer
                    (accept [this t]
                      (callback-fn (-> t convert-query-result rewrite-done))))]
     (.query conn query chunk-size consumer))))

(defn measurements
  "Returns a list of measurements present in a database"
  [^InfluxDB conn ^String database-name]
  (-> (query conn "SHOW MEASUREMENTS" database-name)
      :results
      first
      :series
      first
      :values
      flatten))

(defn series
  "Returns a list of series present in a database"
  [^InfluxDB conn ^String database-name]
  (-> (query conn "SHOW SERIES" database-name)
      :results
      first
      :series
      first
      :values
      flatten))
