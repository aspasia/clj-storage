;; clj-storage - a minimal storage library

;; Copyright (C) 2019- Dyne.org foundation

;; Sourcecode designed, written and maintained by
;; Aspasia Beneti <aspra@dyne.org>

;; This program is free software: you can redistribute it and/or modify
;; it under the terms of the GNU Affero General Public License as published by
;; the Free Software Foundation, either version 3 of the License, or
;; (at your option) any later version.

;; This program is distributed in the hope that it will be useful,
;; but WITHOUT ANY WARRANTY; without even the implied warranty of
;; MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
;; GNU Affero General Public License for more details.

;; You should have received a copy of the GNU Affero General Public License
;; along with this program.  If not, see <http://www.gnu.org/licenses/>.

(ns clj-storage.test.db.sqlite
  (:require [midje.sweet :refer [against-background before after facts fact => truthy]]

            [clj-storage.core :as storage]
            [clj-storage.db.sqlite :as db]
            [clj-storage.test.db.sqlite.test-db :as test-db]
            
            [taoensso.timbre :as log]))

(def headers [:name :appearance :cost :grade])
(def main-table-name "FRUIT")
(def main-columns ["ID INTEGER PRIMARY KEY AUTOINCREMENT"
                   "NAME VARCHAR(32) UNIQUE"
                   "APPEARANCE VARCHAR(32) DEFAULT NULL"
                   "COST INT DEFAULT NULL"
                   "GRADE REAL DEFAULT NULL"
                   "CREATEDAT TIMESTAMP NOT NULL"])
(def secondary-table-name "CLASSIFICATION")
(def secondary-columns ["NAME VARCHAR(32) UNIQUE"
                        "GENUS VARCHAR(32)"])


(against-background [(before :contents (test-db/setup-db))
                     (after :contents (test-db/teardown-db))]

                    (let [fruit-store (db/create-sqlite-table (test-db/get-datasource) main-table-name main-columns)
                          classification-store (db/create-sqlite-table (test-db/get-datasource) secondary-table-name secondary-columns)]

                      (facts "Test the sqlite protocol implemetation"
                             (fact "Table(s) created"
                                   (let [tables (db/show-tables (test-db/get-datasource))]
                                     (count tables) => 2
                                     (-> tables first :sqlite_master/TABLE_NAME) => secondary-table-name
                                     (-> tables second :sqlite_master/TABLE_NAME) => "FRUIT"))

                             (fact "Add index"
                                   (storage/add-index fruit-store
                                                      "idx_fruit_name"
                                                      {:unique true
                                                       :columns "NAME"})
                                   (-> (filter
                                        (comp #(= % "idx_fruit_name") :INDEX_NAME)
                                        (db/retrieve-table-indices (test-db/get-datasource) main-table-name))
                                       first
                                       :COLUMN_NAME)
                                   => "NAME")

                             ;; TODO JOINS
                             #_(fact "Check join between two tables"
                                   )
                             
                             (fact "Insert rows to table"
                                   (storage/store! fruit-store (zipmap headers ["Apple" "red" 59 nil])) => truthy
                                   (storage/store! fruit-store (zipmap headers ["Banana" "yellow" nil 92.2])) => truthy
                                   (storage/store! fruit-store (zipmap headers ["Peach" nil 139 90.0])) => truthy
                                   (storage/store! fruit-store (zipmap headers ["Orange" "juicy" 89 88.6])) => truthy
                                   (storage/store! fruit-store (zipmap headers ["Cherry" "red" nil nil])) => truthy)

                             (fact "Query the table with pagination"
                                   (count (storage/query fruit-store {:id 1} {})) => 1
                                   (:FRUIT/NAME (first (storage/query fruit-store {:id 1} {}))) => "Apple"
                                   (count (storage/query fruit-store {:FRUIT/APPEARANCE "red"} {})) => 2
                                   (count (storage/query fruit-store ["COST > ?" 80] {})) => 2
                                   (count (storage/query fruit-store ["APPEARANCE is null"] {})) => 1
                                   (count (storage/query fruit-store ["CREATEDAT > ?" (java.util.Date. "January 1, 1970, 00:00:00 GMT")] {})) => 5)

                             #_(fact "Update some rows"
                                   )

                             (fact "Test the aggregates"
                                   (vals (storage/aggregate fruit-store nil {:select "COUNT (*)"})) => '(5)
                                   (vals (storage/aggregate fruit-store nil {:select "COUNT (DISTINCT APPEARANCE)"})) => '(3)
                                   (vals (storage/aggregate fruit-store nil {:select "MAX (COST)"})) => '(139))
                             
                             #_(fact "Delete some rows"))

                      (facts "Test the expiration" :slow)
                      ))
