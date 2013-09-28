(ns cljsfiddle.db.schema
  (:require [datomic.api :as d]))

(def schema
  [;; blob
   {:db/id (d/tempid :db.part/db) 
    :db/ident :cljsfiddle.blob/sha
    ;; :db/index true (implied by uniqueness)
    :db/unique :db.unique/value
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db.install/_attribute :db.part/db}

   {:db/id (d/tempid :db.part/db) 
    :db/ident :cljsfiddle.blob/text
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db.install/_attribute :db.part/db}

   ;; src
   {:db/id (d/tempid :db.part/db)
    :db/ident :cljsfiddle.src/blob
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one
    :db.install/_attribute :db.part/db}

   {:db/id (d/tempid :db.part/db)
    :db/ident :cljsfiddle.src/ns
    :db/unique :db.unique/identity
    ;; :db/index true (implied by uniqueness)
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db.install/_attribute :db.part/db}

   {:db/id (d/tempid :db.part/db)
    :db/ident :cljsfiddle.src/requires
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/many
    :db.install/_attribute :db.part/db}

   {:db/id (d/tempid :db.part/db)
    :db/ident :cljsfiddle.src/type
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one
    :db.install/_attribute :db.part/db}
   
   {:db/id (d/tempid :db.part/user)
    :db/ident :cljsfiddle.src.type/cljs}

   {:db/id (d/tempid :db.part/user)
    :db/ident :cljsfiddle.src.type/js}

   {:db/id (d/tempid :db.part/user)
    :db/ident :cljsfiddle.src.type/html}

   {:db/id (d/tempid :db.part/user)
    :db/ident :cljsfiddle.src.type/css}
   
   ;; fiddle
   {:db/id (d/tempid :db.part/db)
    :db/ident :cljsfiddle/cljs
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one
    :db.install/_attribute :db.part/db}
   
   {:db/id (d/tempid :db.part/db)
    :db/ident :cljsfiddle/html
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one
    :db.install/_attribute :db.part/db}

   {:db/id (d/tempid :db.part/db)
    :db/ident :cljsfiddle/css
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one
    :db.install/_attribute :db.part/db}
   
   {:db/id (d/tempid :db.part/db)
    :db/ident :cljsfiddle/updated
    :db/valueType :db.type/instant
    :db/cardinality :db.cardinality/one
    :db.install/_attribute :db.part/db}])
