(ns cljsfiddle.db.src
  (:require [clojure.data :as data]
            [datomic.api :as d]
            [cljsfiddle.db.schema :refer (schema)]
            [cljsfiddle.db.util :refer (tempid?)]
            [cljsfiddle.db.blob :as blob]))

(defn ns-eid
  "Return the entity id for ns, or a tempid if ns does not exist."
  [db ns]
  {:pre [(not (nil? ns))]}
  (or (:e (first (d/datoms db :avet :cljsfiddle.src/ns ns)))
      (d/tempid :db.part/user)))

(defn src-tx
  [db {:keys [src sha ns requires type blob-eid]}]
  (let [{:keys [tx id]} (let [nid (ns-eid db ns)]
                          (if (tempid? nid)
                            {:id nid
                             :tx [{:db/id nid
                                   :cljsfiddle.src/blob blob-eid
                                   :cljsfiddle.src/ns ns
                                   :cljsfiddle.src/requires requires
                                   :cljsfiddle.src/type type}]}
                            (let [code (d/entity db nid)
                                  old-sha (-> code :cljsfiddle.src/blob :cljsfiddle.blob/sha)]
                              (if (not= old-sha sha)
                                (let [old-requires (set (:cljsfiddle.src/requires code))
                                      new-requires (set requires)
                                      [req-retracts req-asserts] (data/diff old-requires new-requires)
                                      req-retracts-tx (for [req req-retracts]
                                                        [:db/retract nid :cljsfiddle.src/requires req])
                                      req-asserts-tx (for [req req-asserts]
                                                       [:db/add nid :cljsfiddle.src/requires req])]
                                  {:id nid
                                   :tx (concat req-retracts-tx
                                               req-asserts-tx
                                               [[:db/add nid :cljsfiddle.src/blob blob-eid]])})
                                {:id nid :tx []}))))]
    {:id id
     :tx tx}))

(defn cljs-tx
  "Returns {:id <eid> :tx <tx-data>} for cljs"
  [db cljs]
  {:pre [(every? (partial contains? cljs)
                 [:src :sha :ns :requires])]
   :post [(every? (partial contains? %) [:tx :id])]}
  (let [{:keys [sha src]} cljs
        {blob-id :id blob-tx :tx} (blob/blob-tx db {:sha sha :text src})]
    (update-in (src-tx db (assoc cljs 
                             :type :cljsfiddle.src.type/cljs
                             :provides [(:ns cljs)]
                             :blob-eid blob-id))
               [:tx]
               (fn [tx] (concat blob-tx tx)))))

;; TODO
(defn js-tx
  "Returns transaction data for for js"
  [db js]
  {:pre [(every? (partial contains? js) 
                 [:src :sha :provides :requires])]
   :post []}
  (let [{:keys [sha src]} js
        {blob-id :id blob-tx :tx} (blob/blob-tx db {:sha sha :text src})] 
    (apply concat
           blob-tx
           (for [ns (:provides js)]
             (:tx (src-tx db (assoc js 
                                :type :cljsfiddle.src.type/js 
                                :ns ns
                                :blob-eid blob-id)))))))

(defn none-namespaced-src-tx
  [db {:keys [src sha type]}]
  (let [{blob-id :id blob-tx :tx} (blob/blob-tx db {:sha sha :text src})
        eid (when-not (tempid? blob-id) 
              (ffirst 
               (d/q '[:find ?e
                      :in $ ?blob ?type
                      :where
                      [?e :cljsfiddle.src/blob ?blob]
                      [?e :cljsfiddle.src/type ?type]]
                    db blob-id type)))]
    (if-not eid
      (let [tid (d/tempid :db.part/user)]
        {:id tid
         :tx (concat blob-tx
                     [{:db/id tid
                       :cljsfiddle.src/type type
                       :cljsfiddle.src/blob blob-id}])}) 
      {:id eid :tx []})))

(defn html-tx
  "Returns {:id <eid> :tx <tx-data>} for html"
  [db html]
  (none-namespaced-src-tx db (assoc html :type :cljsfiddle.src.type/html)))

(defn css-tx
  "Returns {:id <eid> :tx <tx-data>} for css"
  [db css]
  (none-namespaced-src-tx db (assoc css :type :cljsfiddle.src.type/css)))

(comment
  (let [uri "datomic:mem://js-tests"
        conn (do (d/delete-database uri)
                (d/create-database uri)
                (let [conn (d/connect uri)]
                  @(d/transact conn schema)
                  conn))
        js-src-1 {:src "some js source"
                  :sha "heifjioisdj"
                  :provides ["foo" "bar" "baz"]
                  :requires ["a" "b" "c"]}

        js-src-2 {:src "some (modified) js source"
                  :sha "!heifjioisdj"
                  :provides ["foo" "bar" "baz"]
                  :requires ["a" "b" "c"]}

        js-src-3 {:src "some more (more modified) js source"
                  :sha "fsfiosifdsf"
                  :provides ["foo" "bar" "baz"]
                  :requires ["a" "b" "d"]}

        js-src-4 {:src "some (even more modified) js source"
                  :sha "fsiokfgmdoig"
                  :provides ["foo" "bar" "quux"]
                  :requires ["a" "b" "c"]}

        ns->requires (fn [db ns]
                            (set
                             (map first 
                                  (d/q '[:find ?reqs
                                         :in $ ?ns
                                         :where 
                                         [?e :cljsfiddle.src/ns ?ns]
                                         [?e :cljsfiddle.src/requires ?reqs]]
                                       db ns))))
]
    ;; Transact js-src-1
    @(d/transact conn (js-tx (d/db conn) js-src-1))
    (assert (= (ns->requires (d/db conn) "foo")
               #{"a" "b" "c"}))

    @(d/transact conn (js-tx (d/db conn) js-src-2))
    (assert (= (ns->requires (d/db conn) "foo")
               #{"a" "b" "c"}))

    @(d/transact conn (js-tx (d/db conn) js-src-3))
    (assert (= (ns->requires (d/db conn) "foo")
               #{"a" "b" "d"})))

  ;; Some tests
  (use 'clojure.pprint)
  
  (def uri "datomic:mem://js-tests")
  (def conn (do (d/delete-database uri)
                (d/create-database uri)
                (let [conn (d/connect uri)]
                  @(d/transact conn schema)
                  conn)))

  ;; The provide list has changed (What to do?)
  ;; Should I retract old provides? Probably in the js case.
  ;; But that's a TODO for now.
  (def src-4 )

  (def cljs-src-1 {:src "some cljs src"
                   :sha "fjsodidfjs"
                   :ns "foo.bar"
                   :requires []})

  (def cljs-src-2 {:src "some (mod) cljs src"
                   :sha "fjsodsdfsifsdfsdfjs"
                   :ns "foo.bar"
                   :requires ["foo.baz"]})

  @(d/transact conn (js-tx (d/db conn) src-1))

  (-> (js-tx (d/db conn)
             src-2)
      pprint)

  @(d/transact conn (:tx (cljs-tx (d/db conn) cljs-src-2)))

  (-> (cljs-tx (d/db conn)
               cljs-src-2)
      pprint)
  
  (def html-src-1 {:src "some html src"
                   :sha "jfisofjoijfsoij"})

  (def html-src-2 {:src "some (other) html src"
                   :sha "jfisofjoijfsfsdfsoij"})
  

  @(d/transact conn (html-tx (d/db conn) html-src-1))

  (-> (html-tx (d/db conn)
               html-src-1)
      pprint)

  (def css-src-1 {:src "some html src"
                  :sha "jfisofjoijfsoij"})
  
  (def css-src-2 {:src "some (other) html src"
                  :sha "jfisofjoijfsfsdfsoij"})
  
  @(d/transact conn (css-tx (d/db conn) css-src-2))

  (-> (css-tx (d/db conn)
              css-src-2)
      pprint)

  
  )