(ns influxdb-clojure.core-test
  (:require [clojure.test :refer :all]
            [clojure.tools.logging :as log]
            [influxdb-clojure.core :refer :all]
            [clj-time.core :refer [now]]
            [clj-time.coerce :refer [to-long]]
            [version-clj.core :refer [version-compare]]
            [clj-time.core :as t]))

(defn fixture-write-points []
  (let [conn (connect)
        test-db-name "influxdb_clojure_test"
        fields {:value 23}
        point {:measurement "test.write"
               :time        (to-long (t/date-time 2000 1 1))
               :fields      fields}]
    (write-points conn test-db-name [point])))

(defn with-test-db [f]
  (let [conn (connect)
        test-db-name "influxdb_clojure_test"]
    (delete-database conn test-db-name)
    (create-database conn test-db-name)
    (fixture-write-points)
    (f)))

(use-fixtures :each with-test-db)

(deftest ping-test
  (let [conn (connect)
        resp (ping conn)
        version (:version resp)]
    (log/debug "InfluxDB version:" version)
    (is (not (empty? version)))
    (is (> 0 (version-compare "0.9" version)))
    (is (> 100 (:response-time resp)))))

(deftest database-tests
  (testing "database creation and deletion"
    (defn db-exists? [conn db-name]
      (.contains (databases conn) db-name))
    (let [conn (connect)
          db-name (str "create_db_test_" (to-long (now)))]
      (do
        (create-database conn db-name)
        (is (db-exists? conn db-name))
        (delete-database conn db-name)
        (is (not (db-exists? conn db-name)))))))



;; Helper functions for query tests
(defn- count-results [query-result]
  (count (:results query-result)))

(defn- count-series [query-result]
  (let [result-index 0]
    (count (:series (get (:results query-result) result-index)))))

(defn- series-name [query-result]
  (let [first-result (first (:results query-result))
        first-series (first (:series first-result))]
    (:name first-series)))

(defn- series-columns [query-result]
  (let [first-result (first (:results query-result))
        first-series (first (:series first-result))]
    (:colums first-series)))

(defn- series-values [query-result]
  (let [first-result (first (:results query-result))
        first-series (first (:series first-result))]
    (:values first-series)))

(defn- series-nth-value [query-result row-index field]
  (defn positions [pred coll]
    (keep-indexed (fn [idx x] (when (pred x) idx)) coll))
  (let [values (series-values query-result)
        columns (series-columns query-result)
        field-index (first (positions #{field} columns))]
    (get (get values row-index) field-index)))

(deftest query-tests
  (testing "select from series with success"
    (let [conn (connect)
          test-db-name "influxdb_clojure_test"
          query-str "select * from \"test.write\""
          query-result (query conn query-str test-db-name)]
      (log/debug "Query:" query-str "->" query-result)
      (is (= 1 (count-results query-result)))
      (is (= 1 (count-series query-result)))
      (is (= "test.write" (series-name query-result)))
      (is (= ["time" "value"] (series-columns query-result)))
      (is (= 1 (count (series-values query-result))))
      (is (= "2000-01-01T00:00:00Z" (series-nth-value query-result 0 "time")))))
  (testing "query with error"
    (let [conn (connect)
          test-db-name "influxdb_clojure_test"
          query-str "invalid query"]
      (is (thrown? RuntimeException
                   (query conn test-db-name query-str))))))

(deftest measurement-tests
  (testing "show measurements for db"
    (let [conn (connect)
          db-name "influxdb_clojure_test"
          ms (measurements conn db-name)]
      (log/debug "Measurements:" ms)
      (is (= '("test.write") ms)))))

(deftest series-tests
  (testing "show series for db"
    (let [conn (connect)
          db-name "influxdb_clojure_test"
          ss (series conn db-name)]
      (log/debug "Series:" ss)
      (is (= '("test.write") ss)))))

#_ (series (connect) "influxdb_clojure_test")
