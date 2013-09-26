(ns cljsfiddle.db.fiddle
  (:require [clojure.data :as data]
            [datomic.api :as d]
            [cljsfiddle.db.schema :refer (schema)]
            [cljsfiddle.db.util :refer (tempid?)]
            [cljsfiddle.db.blob :as blob]
            [cljsfiddle.db.src :as src]))

(defn fiddle-eid [db ns]
  (or (ffirst (d/q '[:find ?e 
                     :in $ ?ns
                     :where
                     [?c :cljsfiddle.src/ns ?ns]
                     [?e :cljsfiddle/cljs ?c]]
                   db ns))
      (d/tempid :db.part/user)))

(defn fiddle-tx [db
                 {:keys [cljs html css]}]
  (let [fid (fiddle-eid db (:ns cljs))
        fent (d/entity db fid)
        cljs-old-eid (:db/id (:cljsfiddle/cljs fent))
        html-old-eid (:db/id (:cljsfiddle/html fent))
        css-old-eid  (:db/id (:cljsfiddle/css fent))
        {cljs-tx :tx cljs-eid :id} (src/cljs-tx db cljs)
        {html-tx :tx html-eid :id} (src/html-tx db html)
        {css-tx  :tx css-eid  :id} (src/css-tx db css)]
    {:id fid
     :tx (cond-> (vec (concat cljs-tx html-tx css-tx))
            
                 (not= cljs-old-eid cljs-eid) 
                 (conj [:db/add fid :cljsfiddle/cljs cljs-eid])
                 
                 (not= html-old-eid html-eid) 
                 (conj [:db/add fid :cljsfiddle/html html-eid])
                 
                 (not= css-old-eid  css-eid)  
                 (conj [:db/add fid :cljsfiddle/css css-eid]))}))



(comment
  ;; Some tests
  (use 'clojure.pprint)
  
  (def uri "datomic:mem://js-tests")
  (def conn (do (d/delete-database uri)
                (d/create-database uri)
                (let [conn (d/connect uri)]
                  @(d/transact conn schema)
                  conn)))

  
  (def cljs-src-1 {:src "some cljs src"
                   :sha "fjsodidfjs"
                   :ns "foo.bar"
                   :requires []})

  (def cljs-src-2 {:src "some (mod) cljs src"
                   :sha "fjsodsdfsifsdfsdfjs"
                   :ns "foo.bar"
                   :requires ["foo.baz" "foo.quux"]})

  (def css-src-1 {:src "some css src"
                  :sha "fjdsiofjos"})
  
  (def css-src-2 {:src "some other css src"
                  :sha "gjsuureiwef"})

  (def html-src-1 {:src "some html-src"
                   :sha "diosjdfsoijgso"})

  (def html-src-2 {:src "some other html src"
                   :sha "fhisudhfoiase"})


  (def fiddle-1 {:cljs cljs-src-1
                 :html html-src-1
                 :css  css-src-1})

  (def fiddle-2 {:cljs cljs-src-1
                 :html html-src-1
                 :css css-src-2})

  (def fiddle-3 {:cljs cljs-src-2
                 :html html-src-1
                 :css css-src-1})
  

  (-> (fiddle-tx (d/db conn)
                 fiddle-1)
      pprint)

  
  @(d/transact conn (fiddle-tx (d/db conn) fiddle-1))

)