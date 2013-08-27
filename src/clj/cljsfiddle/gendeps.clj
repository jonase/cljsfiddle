(ns cljsfiddle.gendeps
  (:require [cljs.closure :refer [build optimize] :as cljs]
            [clojure.java.io :as io]
            [me.raynes.fs :as fs]))

(def namespaces
  '[cljs.reader
    
    clojure.data clojure.reflect clojure.set clojure.string
    clojure.walk clojuire.zip clojure.core.reducers
    
    domina domina.events domina.css])

(defn output-dir [version] 
  (str "resources/public/deps/" version "/"))

(defn build-ns-form [namespaces]
  (list 'ns (gensym "deps_")
        (cons :require
              (map vector namespaces))))

(defn optimize-ws [dir]
  (doall (fs/walk
          (fn [root dirs files]
            (doseq [file files
                    :when (.endsWith file ".js")]
              (let [file (fs/file root file)
                    js-src (->> (cljs/read-js file)
                                (optimize {:optimizations :whitespace}))]
                (spit file js-src))))
          dir)))

(defn -main [version]
  (println "building...")
  (build [(build-ns-form namespaces)]
         {:output-dir (output-dir version)})
  (println "optimizing...")
  (optimize-ws (output-dir version))
  (println "done."))

;; (-main "1")


