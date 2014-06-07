(ns cljsfiddle.import
  (:require [clojure.string :as s]
            [clojure.java.io :as io]
            [datomic.api :as d]
            [cljs.closure :as cljs]
            [cljs.js-deps :as cljs-deps]
            [environ.core :refer (env)]
            [cljsfiddle.db.src :as src]
            [cljsfiddle.db.schema :refer (schema)]
            [cljsfiddle.db.util :as util :refer (sha
                                        cljs-object-from-file
                                        js-object-from-file)]
            [cljsfiddle.db.fiddle :as fiddle])
  (:import [clojure.lang LineNumberingPushbackReader]
           [java.util Date]
           [java.io StringReader BufferedReader]
           [org.apache.commons.codec.digest DigestUtils]))


(defn create-db [& [uri]]
  (let [uri (or uri
                (env :datomic-uri)
                "datomic:free://localhost:4334/cljsfiddle")]
    (d/create-database uri)
    (let [conn (d/connect uri)]
       @(d/transact conn schema))))

;; Import js and cljs from the classpath into datomic.

(defn find-files [paths jars]
  (filter (fn [file]
            (and (some #(.startsWith file %) paths)
                 (some #(.endsWith file %) [".js" ".cljs"])))
          (mapcat cljs-deps/jar-entry-names* jars)))


(defn goog-base-tx [db]
  (let [goog-base (d/entity db :goog/base)
        old-sha (-> goog-base
                    :cljsfiddle.src/blob
                    :cljsfiddle.blob/sha)
        new-sha (sha (slurp (io/resource "goog/base.js")))
        blob-eid (ffirst (d/q '[:find ?e
                                :in $ ?sha
                                :where
                                [?e :cljsfiddle.blob/sha ?sha]]
                              db new-sha))]
    (assert blob-eid "No goog.base in db")
    (let [src-eid (d/tempid :db.part/user)]
      (cond
       (nil? old-sha)
       [[:db/add src-eid :cljsfiddle.src/type :cljsfiddle.src.type/js]
        [:db/add src-eid :cljsfiddle.src/blob blob-eid]
        [:db/add src-eid :db/ident :goog/base]]

       (not= old-sha new-sha)
       [[:db/add src-eid :cljsfiddle.src/blob blob-eid]]

       :else
       []))))

(defn default-fiddle-tx [db]
  (let [fiddle (util/fiddle "(ns cljsfiddle)\n\n(set! (.-innerHTML (.getElementById js/document \"msg\"))\n      \"CLJSFiddle\")\n"
                            "<p>Hello, <strong id=\"msg\"></strong></p>\n"
                            "p {\n  color: #f41;\n  font-family: helvetica;\n  font-size: 2em;\n  text-align: center\n}")
        {:keys [tx id]} (fiddle/fiddle-tx db fiddle)]
    (println tx id)
    (if (not (empty? tx))
      (cons [:db/add id :db/ident :cljsfiddle/default-fiddle]
            tx)
      tx)))

;; Import cljs and js deps. Idempotent
;; TODO: Figure out if schema is installed.
(defn -main [uri]
  (let [conn (d/connect uri)
        files (find-files #{"cljs/" "clojure/" "goog/" "domina" "hiccups" "dommy"}
                          (filter #(.endsWith % ".jar")
                                  (-> "java.class.path"
                                      System/getProperty
                                      (s/split #":"))))
        js-files (filter #(.endsWith % ".js") files)
        js-objects (map js-object-from-file js-files)
        cljs-files (filter #(.endsWith % ".cljs") files)
        cljs-objects (map cljs-object-from-file cljs-files)]
    (println "transacting cljs")
    (doseq [cljs cljs-objects]
      (let [cljs-tx (:tx (src/cljs-tx (d/db conn) cljs))]
        (print "Considering " (:file cljs) "... ")
        (if-not (empty? cljs-tx)
          (do @(d/transact conn cljs-tx)
              (println "transacted."))
          (println "skipped.")
          )))
    (println "done.")
    (println "transacting js")
    (doseq [js js-objects]
      (let [js-tx (src/js-tx (d/db conn) js)]
        (print "Considering " (:file js) "... ")
        (if-not (empty? js-tx)
          (do @(d/transact conn js-tx)
              (println "transacted."))
          (println "skipped."))))
    (println "done.")
    (println "Special casing goog/base.js")
    (let [tx (goog-base-tx (d/db conn))]
      (when-not (empty? tx)
        (println tx)
        @(d/transact conn tx)))
    (println "Adding default fiddle")
    (let [tx (default-fiddle-tx (d/db conn))]
      (when-not (empty? tx)
        (println tx)
        @(d/transact conn tx)))

    (println "Running storage GC")
    (d/gc-storage conn (Date.))
    (println "Done.")))

;; (-main (env :datomic-uri))

(comment

  (use 'clojure.pprint)
  (def uri (env :datomic-uri))

  (do (d/delete-database uri)
      (d/create-database uri))



  (def conn (d/connect uri))

  @(d/transact conn schema)

  (d/q '[:find ?e :where
         [?e :cljsfiddle.src/ns "goog.string"]]
       (d/db conn))

  (d/entity (d/db conn) :goog/base)

  (d/q '[:find ?sha
         :in $ ?sha
         :where
         [?e :cljsfiddle.blob/sha ?sha]]
       (d/db conn)
       )

  (d/touch (d/entity (d/db conn) :db.type/ref))


  (src/cljs-tx (d/db conn) (cljs-object "domina.cljs"))

  (:cljsfiddle.src/ns (d/entity (d/db conn) 17592186045435))

  (pprint (d/touch (:cljsfiddle.src/blob (d/entity (d/db conn) :goog/base))))

  (d/touch (d/entity (d/db conn) :cljsfiddle/default-fiddle))
  )
