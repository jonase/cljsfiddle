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

(defn make-deps [deps]
  (let [html [[:script "CLOSURE_NO_DEPS=true;"]
              [:script "COMPILED=true;"]]
        ds (for [dep deps]
             [:script {:src (str "/jscache/1/" (s/replace dep ".cljs" ".js"))}])]
    (apply str (map render-html (concat html ds)))))

(defn make-srcdoc [html css js deps]
  (render-html
   [:html
    [:head
     [:style css]]
    [:body
     html
     (make-deps deps)
     [:script js]]]))

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
                                       (alert-success "Fiddle saved successfully!")
                                       (alert-error (:msg res))))})))

    (.on (js/$ "a[data-toggle=\"tab\"]") 
         "shown.bs.tab" 
         (fn [evt]
           (condp = (dom/attr (.-target evt) :href)
             "#cljs-editor-tab" (.refresh cljs-editor)
             "#html-editor-tab" (.refresh html-editor)
             "#css-editor-tab" (.refresh css-editor))))))
