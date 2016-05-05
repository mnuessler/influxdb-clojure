(ns influxdb-clojure.core-test
  (:require [clojure.test :refer :all]
            [clojure.tools.logging :as log]
            [influxdb-clojure.core :refer :all]
            [version-clj.core :refer [version-compare]]))

(defn with-test-db [f]
  (let [conn (connect)
        test-db-name "influxdb_clojure_test"]
    (create-database conn test-db-name)
    (f)))
;    (delete-database conn test-db-name)))

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
          db-name (str "create_db_test_" (System/currentTimeMillis))]
      (do
        (create-database conn db-name)
        (is (db-exists? conn db-name))
        (delete-database conn db-name)
        (is (not (db-exists? conn db-name)))))))

(deftest write-tests
  (testing "write-points"
    (let [conn (connect)
          test-db-name "influxdb_clojure_test"
          fields {:value 23}
          point {:measurement "test.write"
                 :time        (System/currentTimeMillis)
                 :fields      fields}]
      (write-points conn test-db-name [point]))))
