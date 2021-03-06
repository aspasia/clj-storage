;; clj-storage - a minimal storage library

;; part of Decentralized Citizen Engagement Technologies (D-CENT)
;; R&D funded by the European Commission (FP7/CAPS 610349)

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

(ns clj-storage.test.db.redis
  (:require [midje.sweet :refer [against-background before after facts fact => truthy]]

            [clj-storage.core :as storage]
            [clj-storage.db.redis :as redis]
            [clj-storage.test.db.redis.test-db :as test-db]
            [clj-storage.spec]

            [taoensso.timbre :as log]))

(against-background [(before :contents (test-db/setup-db))
                     (after :contents (test-db/teardown-db))]

                    (facts "Test the redis protocol implemetation"
                           (fact "Test mongo stores creation and fetch"
                                 ;; insert document so store is created
                                 (storage/store! (test-db/get-test-store) {:key "foo"
                                                                           :value "bar"}) => "OK"
                                 (storage/query (test-db/get-test-store) {:key "foo"} {}) => "bar"
                                 (redis/count-keys (test-db/get-test-store)) => 1
                                 (redis/get-all-keys (test-db/get-test-store)) => ["foo"]

                                 ;; Try to add other types, for example maps and sets
                                 (storage/store! (test-db/get-test-store) {:key "mymap"
                                                                           :value {:name "my-name"
                                                                                   :age 56}})
                                 (storage/query (test-db/get-test-store) {:key "mymap"} {}) => {:name "my-name"
                                                                                                :age 56}
                                 (storage/store! (test-db/get-test-store) {:key "myset"
                                                                           :value #{:a :b :c}})

                                 (storage/query (test-db/get-test-store) {:key "myset"} {}) => #{:a :b :c})

                           (fact "Add more key-value pairs and do a query that results in multiple"
                                 (doseq [n (range 10)]
                                   (storage/store! (test-db/get-test-store) {:key (java.util.UUID/randomUUID)
                                                                             :value (str "value" n)}) => "OK")
                                 (redis/count-keys (test-db/get-test-store)) => 13

                                 (let [ks (redis/get-all-keys (test-db/get-test-store))]
                                   (count (storage/query (test-db/get-test-store) {:keys (take 5 ks)} {})) => 5))
                           (fact "Test one key value atomic update."
                                 
                                 (storage/query (test-db/get-test-store) {:key "foo"} {}) => "bar"
                                 ;; WHat is should be without updating in redis
                                 (let [update-fn #(str % "bar")]
                                   (update-fn (storage/query (test-db/get-test-store) {:key "foo"} {})) => "barbar"
                                   ;; Now update and check if it is indeed the same
                                   (storage/update! (test-db/get-test-store) {:key "foo"} update-fn)
                                   (count (redis/get-all-keys (test-db/get-test-store))) => 13
                                   (storage/query (test-db/get-test-store) {:key "foo"} {}) => "barbar")
                                 ;; TODO: maybe spawn a thread that changes in the same time, check that lock works indeed
                                 )
                           (fact "Test multiple values atomic update."
                                 (let [some-keys (take 3 (redis/get-all-keys (test-db/get-test-store)))
                                       their-values (storage/query (test-db/get-test-store) {:keys some-keys} {})
                                       update-fn #(str % "bar")
                                       updated-values (mapv (fn [v] (update-fn v)) their-values)]

                                   (storage/update! (test-db/get-test-store) {:keys some-keys} update-fn)
                                   (storage/query (test-db/get-test-store) {:keys some-keys} {}) => updated-values))
                           (facts "Adda secondary index which applies only for sorted sets (will create one if not already existing)"
                                  (storage/add-index (test-db/get-test-store) "sorted-set" {:member :c :score 1}) => 1
                                  (count (redis/get-all-keys (test-db/get-test-store))) => 14
                                  (redis/count-sorted-set (test-db/get-test-store) "sorted-set") => 1
                                  (storage/add-index (test-db/get-test-store) "sorted-set" {:member "lala" :score 2}) => 1
                                  (redis/count-sorted-set (test-db/get-test-store) "sorted-set") => 2)

                           (facts "Test expiration" :slow
                                  (fact "Wait for 10 seconds to check that item is deleted after expiration" :slow
                                        
                                        (-> (storage/query (test-db/get-test-store) {:key "foo"} {})
                                            (clojure.string/starts-with? "barbar")) = true

                                        (storage/expire (test-db/get-test-store) 3 {:keys ["foo"]}) => 1
                                        
                                        ;; TTL  for redis should be accuate to the second "Since Redis 2.6 the expire error is from 0 to 1 milliseconds."
                                        (Thread/sleep (* 10 1000))
                                        (storage/query (test-db/get-test-store) {:key "foo"} {}) => nil))))
