(ns cljsfiddle.core
  (:require [clojure.string :as s]
            [domina :as dom]
            [domina.css :as css]
            [domina.events :as events]
            [hylla.remote :as remote]))

(defn ends-with? [string suffix]
  (not= -1 (.indexOf string suffix (- (.-length string) (.-length suffix)))))

(defn code-mirror [id opts]
  (.fromTextArea js/CodeMirror (dom/by-id id) (clj->js opts)))

(defn make-deps [deps]
  (apply str "<script>CLOSURE_NO_DEPS=true;</script><script src=\"/deps/1/goog/base.js\"></script><script>COMPILED=true</script>"
         (for [dep deps]
           (format "<script src=\"/deps/1/%s\"></script>" (s/replace dep ".cljs" ".js")))))

(defn make-srcdoc [html css js deps]
  (format "<html><head><style>%s</style></head><body>%s</body>%s<script>%s</script></html>" 
          css 
          html 
          (make-deps deps)
          js))

(defn alert [type msg]
  (let [loc (dom/by-id "alert")]
    (dom/destroy-children! loc)
    (dom/append! loc
                 (format "<div class=\"alert alert-%s fade_in\">%s<a class=\"close\" data-dismiss=\"alert\" href=\"#\" aria-hidden=\"true\">&times;</a></div>" type msg))))

(defn alert-success [msg]
  (alert "success" msg))

(defn alert-error [msg]
  (alert "danger" msg))

(defn ^:export init
  [] 
  (alert-error "This is work-in-progress. Expect bugs/downtime and don't expect your saved work to be durable.")

  (let [html-editor (code-mirror "html-editor" {:lineNumbers true})
        css-editor (code-mirror "css-editor" {:mode :css :lineNumbers true})
        cljs-editor (code-mirror "cljs-editor" {:mode :clojure :lineNumbers true})
        result-frame (domina/by-id "result-frame")
        run-btn (domina/by-id "run-btn")
        save-btn (domina/by-id "save-btn")]
    
    (.setSize cljs-editor "100%" "455px")
    (.setSize html-editor "100%" "455px")
    (.setSize css-editor  "100%" "455px")

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
                                     (if (= (:status res) :success)
                                       (alert-success "Fiddle saved successfully!")
                                       (alert-error (:msg res)))))))

    (.on (js/$ "a[data-toggle=\"tab\"]") 
         "shown.bs.tab" 
         (fn [evt]
           (condp = (dom/attr (.-target evt) :href)
             "#cljs-editor-tab" (.refresh cljs-editor)
             "#html-editor-tab" (.refresh html-editor)
             "#css-editor-tab" (.refresh css-editor))))))
