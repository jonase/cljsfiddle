(ns cljsfiddle.db.blob
  (:require [cljsfiddle.db.util :refer (tempid?)] 
            [datomic.api :as d]))

(defn- blob-eid [db sha]
  "Returns the entity id for sha, or a tempid if sha does not exist"
  (or (:e (first (d/datoms db :avet :cljsfiddle.blob/sha sha)))
      (d/tempid :db.part/user)))

(defn blob-tx 
  "Return {:id <entity-id> :tx <tx-data> for blob and sha"
  [db {:keys [text sha] :as blob}]
  (let [eid (blob-eid db sha)]
    (if (tempid? eid)
      {:id eid
       :tx [{:db/id eid
             :cljsfiddle.blob/sha sha
             :cljsfiddle.blob/text text}]}
      {:id eid
       :tx []})))
