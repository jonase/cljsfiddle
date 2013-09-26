(ns cljsfiddle.db
  (:require [cljsfiddle.db.fiddle :as fiddle]
            [cljsfiddle.db.util :as util]
            [datomic.api :as d]
            [environ.core :refer (env)]))

(defn requires [db ns]
  (map first
       (d/q '[:find ?requires
              :in $ ?ns
              :where 
              [?e :cljsfiddle.src/ns ?ns]
              [?e :cljsfiddle.src/requires ?requires]]
            db ns)))

(defn- toposort* [node dag sorted temporary visited]
  (when-not (@visited node)
    (when (@temporary node) (throw (ex-info "Not a DAG." {})))
    (swap! temporary conj node)
    (doseq [n (requires dag node)]
      (toposort* n dag sorted temporary visited))
    (swap! visited conj node)
    (swap! sorted conj node)))

(defn toposort [node dag]
  (let [sorted (atom [])]
    (toposort* node dag sorted (atom #{}) (atom #{}))
    @sorted))

(defn dependencies 
  "Returns the dependencies of ns in dependency order"
  [db ns]
  (butlast (toposort ns db)))

(defn sha-by-ns [db ns]
  (ffirst (d/q '[:find ?sha
                 :in $ ?ns
                 :where
                 [?e :cljsfiddle.src/ns ?ns]
                 [?e :cljsfiddle.src/blob ?b]
                 [?b :cljsfiddle.blob/sha ?sha]]
               db ns)))

(defn dependency-files 
  [db ns]
  (let [files (distinct
               (for [ns (dependencies db ns)]
                 (let [sha (sha-by-ns db ns)]
                   (str sha ".js"))))
        base (-> (d/entity db :goog/base)
                 :cljsfiddle.src/blob
                 :cljsfiddle.blob/sha)]
    (cons (str base ".js") 
          files)))

(defn save-fiddle [conn fiddle]
  (let [db (d/db conn)
        tx (fiddle/fiddle-tx db fiddle)]
    (if-not (empty? tx)
      (:db-after @(d/transact conn tx))
      db)))

(defn find-fiddle-by-ns [db ns]
  (let [query '[:find ?fiddle
                :in $ ?ns
                :where
                [?src :cljsfiddle.src/ns ?ns]
                [?fiddle :cljsfiddle/cljs ?src]]
        fiddle-id (ffirst (d/q query db ns))]
    (when fiddle-id
      (d/touch (d/entity db fiddle-id)))))


(comment 
  (def db (-> :datomic-uri
              env
              d/connect
              d/db))

  (requires db "cljs.reader")

  (dependencies db "cljs.core")

  (:cljsfiddle.src/ns (:cljsfiddle/cljs (find-fiddle-by-ns db "foo.bar")))
  
  
  (util/fiddle "(ns foo.bar) (defn add [x y] (+ x y))"
               "<html></html>"
               "body {}"))

