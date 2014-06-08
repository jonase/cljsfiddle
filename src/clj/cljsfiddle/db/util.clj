(ns cljsfiddle.db.util
  (:require [clojure.string :as s]
            [clojure.java.io :as io]
            [clojure.tools.reader :as reader]
            [datomic.api :as d]
            [cljs.env :as cljs-env]
            [cljs.closure :as closure]
            [cljs.js-deps :as cljs-deps]
            [cljs.tagged-literals :as tags]
            [environ.core :refer (env)])
  (:import [clojure.lang LineNumberingPushbackReader]
           [java.io StringReader BufferedReader]
           [org.apache.commons.codec.digest DigestUtils]))

(def tempid? map?)

(defn- read-all* [^LineNumberingPushbackReader reader result eof]
  (let [form (reader/read reader false eof)]
    (if (= form eof)
      result
      (recur reader (conj result form) eof))))

(defn read-all [src]
  (binding [reader/*read-eval* false
            reader/*data-readers* tags/*cljs-data-readers*]
    (read-all* (LineNumberingPushbackReader. (StringReader. src))
               []
               (Object.))))

(defn sha [s]
  (DigestUtils/shaHex s))

(defn parse-js-ns [js-src]
  (-> js-src
      StringReader.
      BufferedReader.
      line-seq
      cljs-deps/parse-js-ns))

(defn cljs-object-from-src [cljs-src-str]
  (let [cljs-src (read-all cljs-src-str)
        js-src (cljs-env/with-compiler-env
                 (cljs-env/default-compiler-env)
                 (closure/-compile cljs-src {})) ;; TODO perf.
        {:keys [provides requires]} (parse-js-ns js-src)]
    {:src cljs-src-str
     :js-src js-src
     :sha (sha cljs-src-str)
     :ns (first provides)
     :requires (set requires)}))

(defn cljs-object-from-file [cljs-file]
  (let [cljs-src-str (slurp (io/resource cljs-file))
        cljs-object (cljs-object-from-src cljs-src-str)]
    (assoc cljs-object
      :file cljs-file)))

(defn js-object-from-file [js-file]
  (let [js-src-str (slurp (io/resource js-file))
        {:keys [provides requires]} (parse-js-ns js-src-str)]
    {:file js-file
     :src js-src-str
     :sha (sha js-src-str)
     :provides provides
     :requires requires}))

(defn css-object-from-src [css-src]
  {:src css-src
   :sha (sha css-src)})

(defn html-object-from-src [html-src]
  {:src html-src
   :sha (sha html-src)})

(defn fiddle [cljs html css]
  {:cljs (cljs-object-from-src cljs)
   :html (html-object-from-src html)
   :css  (css-object-from-src css)})
