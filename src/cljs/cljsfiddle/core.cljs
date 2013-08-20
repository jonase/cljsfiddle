(ns cljsfiddle.core
  (:require [clojure.string :as s]
            [domina :as dom]
            [domina.events :as events]
            [hylla.remote :as remote]))

(defn code-mirror [id opts]
  (.fromTextArea js/CodeMirror (dom/by-id id) (clj->js opts)))

(defn make-deps [deps]
  (apply str "<script>CLOSURE_NO_DEPS=true;</script><script src=\"/compiler/deps/1/goog.base\"></script><script>COMPILED=true</script>"
         (for [dep deps]
           (format "<script src=\"/compiler/deps/1/%s\"></script>" dep))))

(defn make-srcdoc [html css js deps]
  (format "<html><head><style>%s</style></head><body>%s</body>%s<script>%s</script></html>" 
          css 
          html 
          (make-deps deps)
          js))

(defn init
  [] 
  (let [html-editor (code-mirror "html-editor" {:lineNumbers true})
        css-editor (code-mirror "css-editor" {:mode :css :lineNumbers true})
        cljs-editor (code-mirror "cljs-editor" {:mode :clojure :lineNumbers true})
        result-frame (domina/by-id "result-frame")
        run-btn (domina/by-id "run-btn")
        save-btn (domina/by-id "save-btn")]
    (.setSize html-editor "100%" "150px")
    (.setSize css-editor "100%" "150px")
    (events/listen! run-btn :click
                    (fn [e] 
                      (remote/post "/compiler/compile" {:src (.getValue cljs-editor)}
                                   (fn [res]
                                     (let [srcdoc (make-srcdoc (.getValue html-editor)
                                                               (.getValue css-editor)
                                                               (:js-src res)
                                                               (:dependencies res))]
                                       (.setAttribute result-frame "srcdoc" srcdoc))))))

    (events/listen! save-btn :click
                    (fn [e]
                      (remote/post "/save" {:fiddle/cljs (.getValue cljs-editor)
                                            :fiddle/html (.getValue html-editor)
                                            :fiddle/css (.getValue css-editor)}
                                   (fn [res]
                                     (.log js/console (pr-str res))))))))
