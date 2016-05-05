(ns influxdb-clojure.core-test
  (:require [clojure.test :refer :all]
            [influxdb-clojure.core :refer :all]))

(defn with-test-db [f]
  (let [conn (connect)
        test-db-name "influxdb_clojure_test"]
    (create-database conn test-db-name)
    (f)))
;    (delete-database conn test-db-name)))

(use-fixtures :each with-test-db)

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
