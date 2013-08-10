(ns cljsfiddle.closure
  (:require [clojure.java.io :as io]
            [clojure.string :as s]
            [clojure.set :as set]
            [cljs.closure :as cljs]
            [clojure.pprint :refer [pprint]]
            [alandipert.kahn :refer [kahn-sort]])
  (:import [clojure.lang LineNumberingPushbackReader]
           [java.util.logging Level]
           [java.io StringReader BufferedReader]
           [com.google.javascript.jscomp.Compiler]
           [com.google.javascript.jscomp JSSourceFile
                                         CompilerOptions
                                         CompilationLevel
                                         ClosureCodingConvention]))

(defn- read-all* [^LineNumberingPushbackReader reader result eof]
  (let [form (read reader false eof)]
    (if (= form eof)
      result
      (recur reader (conj result form) eof))))

(defn- read-all [src]
  (binding [*read-eval* false]
    (read-all* (LineNumberingPushbackReader. (StringReader. src))
               []
               (Object.))))

(defn compile-cljs* [cljs-src-str]
  (let [cljs-src (read-all cljs-src-str)
        js-src (cljs/-compile cljs-src {})
        {:keys [provides requires]} (-> js-src StringReader. BufferedReader. line-seq cljs/parse-js-ns)]
    {:js-src js-src
     :provides (set provides)
     :requires (set requires)}))

(defn find-files [paths jars]
  (filter (fn [file]
            (and (some #(.startsWith file %) paths)
                 (some #(.endsWith file %) [".js" ".cljs"])))
          (mapcat cljs/jar-entry-names*
                  jars)))

(defn cljs-file? [file]
  (.endsWith file ".cljs"))

(defn build-index [paths jars]
  (let [files (find-files paths jars)]
    (apply merge
           (mapcat (fn [file] 
                     (let [{:keys [provides requires]} (if (cljs-file? file)
                                                         (compile-cljs* (slurp (io/resource file)))
                                                         (cljs/parse-js-ns (line-seq (io/reader (io/resource file)))))]
                       (for [provide provides]
                         {provide {:requires (set requires)
                                   :file file}})))
                   files))))

;; Build an index searching through all jars on classpath
(def deps-index (build-index #{"cljs/" "clojure/" "goog/" "domina"} 
                             (filter #(.endsWith % ".jar") 
                                     (-> "java.class.path" System/getProperty (s/split #":")))))

(defn ns-file [ns index]
  (get-in index [ns :file]))

(defn ns-requires [ns index]
  (get-in index [ns :requires]))


(defn js-errors [error]
  {:description (.description error)
   :file (.sourceName error)
   :line (.lineNumber error)})

;; TODO opts & warnings
(defn make-compiler [opts]
  (fn mk-compiler
    ([src] (mk-compiler "__NO_SOURCE_FILE__" src))
    ([name src]
       (let [options (let [level CompilationLevel/WHITESPACE_ONLY
                                  compiler-options (CompilerOptions.)]
                              (.setCodingConvention compiler-options (ClosureCodingConvention.))
                              (.setOptionsForCompilationLevel level compiler-options)
                              compiler-options)
             compiler (com.google.javascript.jscomp.Compiler.) 
             src (JSSourceFile/fromCode name src)
             externs (JSSourceFile/fromCode "externs" "")
             result (.compile compiler externs src options)]
         (if (.success result)
           (merge {:status :success
                   :file name
                   :js-src (.toSource compiler)}
                  (when-let [warnings (seq (.warnings result))]
                    {:warnings (mapv js-errors warnings)}))
           (merge {:status :error
                   :file name
                   :errors (mapv js-errors (.errors result))}
                  (when-let [warnings (seq (.warnings result))]
                    {:warnings (mapv js-errors warnings)})))))))

(def closure-compile (make-compiler {:optimizations :whitespace}))

(defn compile-namespace* [ns index]
  (let [file (ns-file ns index)]
    (closure-compile 
     file
     (if (cljs-file? file)
       (:js-src (compile-cljs* (slurp (io/resource file))))
       (slurp (io/resource file))))))

(defn deps [namespace index]
  (let [requires (set (ns-requires namespace index))]
    (let [transitive-requires (when (seq requires)
                                (set (mapcat #(deps % index) requires)))]
      (set/union requires transitive-requires))))

(defn dependencies* [root index]
  (let [keys (conj (deps root index) root)
        prep (into {}
                   (map (fn [[ns {r :requires}]]
                          [ns r])
                        (select-keys index keys)))
        nss (rseq (kahn-sort prep))]
    (vec (distinct (map #(get-in index [% :file]) nss)))))

(defn compile-cljs
  ([src] (compile-cljs src deps-index))
  ([src index] 
     (let [{:keys [js-src provides requires] :as res} (compile-cljs* src)
           provides (first provides)
           new-index (assoc index provides {:requires requires})]
       (assoc res
         :dependencies (butlast (dependencies* provides new-index))))))

(def dependencies (memoize #(dependencies* % deps-index)))
(def compile-namespace (memoize #(compile-namespace* % deps-index)))

(defn compile-file [file]
  (closure-compile 
   file
   (if (cljs-file? file)
     (:js-src (compile-cljs* (slurp (io/resource file))))
     (slurp (io/resource file)))))

(dependencies "domina")
