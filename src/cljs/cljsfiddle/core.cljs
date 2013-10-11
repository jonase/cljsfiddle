(ns cljsfiddle.core
  (:require [clojure.string :as s]
            [domina :as dom]
            [domina.css :as css]
            [domina.events :as events]
            [hylla.remote :as remote]
            [ajax.core :as http]
            [hiccups.runtime :refer (render-html)]))

(defn ends-with? [string suffix]
  (not= -1 (.indexOf string suffix (- (.-length string) (.-length suffix)))))

(defn code-mirror [id opts]
  (.fromTextArea js/CodeMirror (dom/by-id id) (clj->js opts)))

(defn make-deps [deps version]
  (let [html [[:script "CLOSURE_NO_DEPS=true;"]
              [:script "COMPILED=true;"]]
        ds (for [dep deps]
             [:script {:src (str "/jscache/" version "/" (s/replace dep ".cljs" ".js"))}])]
    (apply str (map render-html (concat html ds)))))

(defn make-srcdoc [html css js deps version]
  (render-html
   [:html
    [:head
     [:style css]]
    [:body
     html
     (make-deps deps version)
     [:script js]
     [:script "parent.postMessage('hi', 'http://localhost:8080')"]]]))

(defn alert [type msg]
  (let [loc (dom/by-id "alert")]
    (dom/destroy-children! loc)
    (dom/append! loc
                 (render-html
                  [:div {:class (str "alert alert-" type " fade in")}
                   msg
                   [:a.close {:data-dismiss "alert"
                              :href "#"
                              :aria-hidden "true"}
                    "&times;"]]))))

(defn alert-success [msg]
  (alert "success" msg))

(defn alert-error [msg]
  (alert "danger" msg))

(def saved? (atom false))

(defn toggle-saved! []
  (let [btn (css/sel "#save-btn")
        span (css/sel "#save-btn span")]
    (dom/set-style! btn :background-color "lightgreen")
    (dom/remove-class! span "glyphicon-floppy-save")
    (dom/add-class! span "glyphicon-floppy-saved")))

(defn toggle-not-saved! []
  (let [btn (css/sel "#save-btn")
        span (css/sel "#save-btn span")]
    (dom/set-style! btn :background-color "white")
    (dom/remove-class! span "glyphicon-floppy-saved")
    (dom/add-class! span "glyphicon-floppy-save")))

(defn editor-content-changed [e]
  (when @saved?
    (toggle-not-saved!)
    (swap! saved? not)))

(defn register-change-listeners [& editors]
  (doseq [editor editors]
    (.on editor "change" editor-content-changed)))

(defn output-fn [editor]
  (let [lines (atom [])]
   (fn [msg]
     (swap! lines conj msg)
     (.setValue editor (s/join "\n" @lines)))))

(defn ^:export init
  [version] 
  
  (let [html-editor (code-mirror "html-editor" {:lineNumbers true})
        css-editor (code-mirror "css-editor" {:mode :css :lineNumbers true})
        cljs-editor (code-mirror "cljs-editor" {:mode :clojure 
                                                :lineNumbers true 
                                                :autoCloseBrackets true 
                                                :matchBrackets true
                                                :cljsfiddleButtons true})
        output-editor (code-mirror "output" {:lineNumbers true})
        result-frame (domina/by-id "result-frame")
        run-btn (domina/by-id "run-btn")
        save-btn (domina/by-id "save-btn")

        output (output-fn output-editor)]
    
    (.setSize cljs-editor "100%" "510px")
    (.setSize html-editor "100%" "510px")
    (.setSize css-editor  "100%" "510px")
    (.setSize output-editor "100%" "130px")

    (set! (.-log js/console) (fn [msg] (output msg)))

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
                                                                (:dependencies res)
                                                                version)]
                                        (.setAttribute result-frame "srcdoc" srcdoc))
                                      :exception
                                      (output (:msg res))))})))
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
                                      (do (reset! saved? true) 
                                          (toggle-saved!)
                                          (output "Message saved successfully!"))
                                      (alert-error (:msg res))))})))

    (.on (js/$ "a[data-toggle=\"tab\"]") 
         "shown.bs.tab" 
         (fn [evt]
           (condp = (dom/attr (.-target evt) :href)
             "#cljs-editor-tab" (.refresh cljs-editor)
             "#html-editor-tab" (.refresh html-editor)
             "#css-editor-tab" (.refresh css-editor))))

    (register-change-listeners cljs-editor html-editor css-editor)

    (.addEventListener js/document
                       "message"
                       (fn [x] 
                         (.log js/console "HI")) 
                       false)))
