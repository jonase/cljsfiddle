(ns cljsfiddle.db
  (:require [datomic.api :as d]))

(defn create [conn schema]
  ;; TODO migrations
  (d/transact conn schema))

(defn upsert [conn fiddle]
  (d/transact conn [(assoc fiddle :db/id (d/tempid :db.part/user))]))

(defn find-by-ns [db ns]
  (when-let [e (:e (first (d/datoms db :avet :fiddle/ns ns)))]
    (dissoc (into {} (d/touch (d/entity db e))) :db/id)))

(def schema
  [{:db/id #db/id[:db.part/db]
    :db/ident :fiddle/ns
    :db/unique :db.unique/identity
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db.install/_attribute :db.part/db}
   
   {:db/id #db/id[:db.part/db]
    :db/ident :fiddle/cljs
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db.install/_attribute :db.part/db}
   
   {:db/id #db/id[:db.part/db]
    :db/ident :fiddle/html
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db.install/_attribute :db.part/db}

   {:db/id #db/id[:db.part/db]
    :db/ident :fiddle/css
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db.install/_attribute :db.part/db}])


(comment ;; Testing
  (def uri "datomic:mem://fiddles")
  (def conn (do (d/delete-database uri)
                (d/create-database uri)
                (d/connect uri)))
  
  (create conn schema)

  (upsert conn
          {:fiddle/ns "jonase.test"
           :fiddle/cljs "cljs"
           :fiddle/css "css code"
           :fiddle/html "html code"})

  (find-by-ns (d/db conn) "jonase.test")

  
  
  )