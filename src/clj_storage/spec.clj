;; clj-storage - Minimal storage library based on an abstraction which helps seamlessly switching between DBs

;; part of Decentralized Citizen Engagement Technologies (D-CENT)
;; R&D funded by the European Commission (FP7/CAPS 610349)

;; Copyright (C) 2019- Dyne.org foundation

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

(ns clj-storage.spec
  (:require [clojure.spec.alpha :as spec]))

;; TODO: extract config
(def MAX-PER-PAGE 100)

(spec/def ::k string?)
(spec/def :clj-storage.db.redis/base-params (spec/keys :req [::k]))

(spec/def ::id string?)
(spec/def ::item map?)
(spec/def :clj-storage.db.mongo/store-params (spec/keys :opt [::id]))
(spec/def :clj-storage.db.mongo/store (spec/keys :req [::store ::item :clj-storage.db.mongo/store-params]))
#_(spec/def ::col string?)
#_(spec/def ::mongodb map?)
#_(spec/def :clj-storage.db.mongo/base-params (spec/keys :req [::mongo-db ::col]))

#_(spec/def :aggregate-type  #{::count})
#_(spec/def :clj-storage.core/aggregate-params (spec/keys ::aggregate-type ::since))
#_(spec/def ::page (spec/int-in 0 Integer/max))
#_(spec/def ::per-page (spec/int-in 1 MAX-PER-PAGE))
#_(spec/def :clj-storage.core/pagination-params (spec/keys ::page ::per-page))
