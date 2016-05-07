(ns influxdb-clojure.core
  (:require [clojure.walk :as walk]
            [clojure.string :refer [upper-case]]
            [clj-time.core :as t])
  (:import (org.influxdb InfluxDB InfluxDBFactory InfluxDB$ConsistencyLevel)
           (org.influxdb.dto BatchPoints Point BatchPoints$Builder Point$Builder Query QueryResult QueryResult$Result QueryResult$Series)
           (java.util.concurrent TimeUnit)
           (retrofit.client OkClient)
           (com.squareup.okhttp OkHttpClient)))

(def ^:private default-uri "http://localhost:8086")

(def ^:private default-user "root")

(def ^:private default-password "root")

(def ^:private connection-default-opts
  {:connect-timeout (* 1 1000)
   :read-timeout    (* 5 1000)
   :write-timeout   (* 1 1000)})

(defn- default-client [opts]
  (let [{:keys [connect-timeout
                read-timeout
                write-timeout]} (merge connection-default-opts opts)
        ^OkHttpClient http-client (OkHttpClient.)
        ^TimeUnit time-unit (TimeUnit/MILLISECONDS)]
    (doto http-client
      (.setConnectTimeout connect-timeout time-unit)
      (.setReadTimeout read-timeout time-unit)
      (.setWriteTimeout write-timeout time-unit))
    (OkClient. http-client)))

(defn connect
  "Connects to the given InfluxDB endpoint and returns a connection"
  (^InfluxDB []
   (connect default-uri))
  (^InfluxDB [uri]
   (connect uri default-user default-password))
  (^InfluxDB [uri user password]
   (connect uri user password {}))
  (^InfluxDB [uri user password opts]
    ;; Create our own OkHttpClient so that we can set connection parameters.
    ;; The default timeouts set by the underlying Retrofit/OkHttpClient implementation
    ;; are rather high: connect timeout: 15s, read timeout: 20s.
    ;; See: retrofit.client.Defaults.
   (let [{:keys [client]
          :or   {client (default-client opts)}} opts]
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

(defn query
  "Executes a database query"
  ([^InfluxDB conn ^String query-str]
   (query conn query-str nil))
  ([^InfluxDB conn ^String query-str ^String database-name]
   (defn convert-series [^QueryResult$Series series]
     (assoc {} :name (.getName series)
               :colums (into [] (.getColumns series))
               :values (into [] (map #(into [] %) (.getValues series)))))
   (defn convert-result [^QueryResult$Result result]
     (if (.hasError result)
       {:error (.getError result)})
     (assoc {} :series (into [] (map convert-series (.getSeries result)))))
   (let [^Query query (Query. query-str database-name)
         ^QueryResult query-result (.query conn query)
         response {}]
     (if (.hasError query-result)
       (assoc response :error (.getError query-result)))
     (assoc response :results (into [] (map convert-result (.getResults query-result)))))))

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
