;; clj-storage - a minimal storage library

;; part of Decentralized Citizen Engagement Technologies (D-CENT)
;; R&D funded by the European Commission (FP7/CAPS 610349)

;; Copyright (C) 2017 Dyne.org foundation

;; Sourcecode designed, written and maintained by
;; Aspasia Beneti  <aspra@dyne.org>

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

(ns clj-storage.core)

(defprotocol Store
  (store! [e k item]
    "Store item against the key k")
  (store-and-create-id! [e item]
    "Store item and return with :_id created by mongo")
  (update! [e k update-fn]
    "Update the item found using key k by running the update-fn on it and storing it")
  (fetch [e k]
    "Retrieve item based on primary id")
  (query [e query]
    "Items are returned using a query map")
  (delete! [e k]
    "Delete item based on primary id")
  (delete-all! [e]
    "Delete all items from a coll")
  (aggregate [e formula]
    "Process data records and return computed results")
  (count-since [e date-time formula]
    "Count the number of records that since a date-time and after applying a formula. {} for an empty formula. This is meant only for collections that contain a `created-at` field.")) 

(defrecord MemoryStore [data]
  Store
  (store! [this k item]
    (do (swap! data assoc (k item) item)
        item))

  (update! [this k update-fn]
    (when-let [item (@data k)]
      (let [updated-item (update-fn item)]
        (swap! data assoc k updated-item)
        updated-item)))

  (fetch [this k] (@data k))

  (query [this query]
    (filter #(= query (select-keys % (keys query))) (vals @data)))

  (delete! [this k]
    (swap! data dissoc k))

  (delete-all! [this]
    (reset! data {}))

  (count-since [this date-time formula]
    ;; TODO: date time add
    (count (filter #(= query (select-keys % (keys query))) (vals @data)))))

(defn create-memory-store
  "Create a memory store"
  ([] (create-memory-store {}))
  ([data]
   ;; TODO: implement ttl and aggregation
   (MemoryStore. (atom data))))

(defn create-in-memory-stores [store-names]
  (zipmap
   (map #(keyword %) store-names)
   (repeat (count store-names) (create-memory-store))))

(defn empty-db-stores! [stores-m]
  (doseq [col (vals stores-m)]
    (delete-all! col)))
