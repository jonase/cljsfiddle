(ns cljsfiddle.core
  (:require [clojure.string :as s]
            [domina :as dom]
            [domina.css :as css]
            [domina.events :as events]
            [hylla.remote :as remote]
            [ajax.core :as http]))

(defn ends-with? [string suffix]
  (not= -1 (.indexOf string suffix (- (.-length string) (.-length suffix)))))

(defn code-mirror [id opts]
  (.fromTextArea js/CodeMirror (dom/by-id id) (clj->js opts)))

(defn make-deps [deps]
  (apply str "<script>CLOSURE_NO_DEPS=true;</script><script>COMPILED=true</script>"
         (for [dep deps]
           (format "<script src=\"/jscache/1/%s\"></script>" (s/replace dep ".cljs" ".js")))))

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
                      (dom/add-class! run-btn "disabled")
                      (http/POST "/compiler/compile"
                        {:params {:src (.getValue cljs-editor)}
                         :handler (fn [res]
                                    (dom/remove-class! run-btn "disabled")
                                    (condp = (:status res)
                                      :ok
                                      (let [srcdoc (make-srcdoc (.getValue html-editor)
                                                                (.getValue css-editor)
                                                                (:js-src res)
                                                                (:dependencies res))]
                                        (.setAttribute result-frame "srcdoc" srcdoc))
                                      :exception
                                      (alert-error (:msg res))))})))
    (events/listen! save-btn :click
                    (fn [e]
                      (dom/add-class! save-btn "disabled")
                      (http/POST "/save"
                        {:params {:cljs (.getValue cljs-editor)
                                  :html (.getValue html-editor)
                                  :css (.getValue css-editor)}
                         :handler (fn [res]
                                    (dom/remove-class! save-btn "disabled")
                                    (if (= (:status res) :success)
                                       (alert-success (format "Fiddle saved successfully! 
                                                               <a href=\"/view/%s?as-of=%s\">[permalink to view]</a>" 
                                                              (:ns res) (:date res)))
                                       (alert-error (:msg res))))})))

    (.on (js/$ "a[data-toggle=\"tab\"]") 
         "shown.bs.tab" 
         (fn [evt]
           (condp = (dom/attr (.-target evt) :href)
             "#cljs-editor-tab" (.refresh cljs-editor)
             "#html-editor-tab" (.refresh html-editor)
             "#css-editor-tab" (.refresh css-editor))))))
