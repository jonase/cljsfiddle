(ns cljsfiddle.views
  (:require [hiccup.util :refer (escape-html)]))

(defn base [& content] 
  [:html {:lang "en"}
   [:head
    [:title "CLJSFiddle"]
    [:meta {:name "viewport"
            :content "width=device-width, initial-scale=1.0"}]
    [:link {:rel "stylesheet"
            :href "//netdna.bootstrapcdn.com/bootstrap/3.0.0-rc1/css/bootstrap.min.css"}]
    [:link {:rel "stylesheet"
            :href "/css/codemirror.css"}]
    [:link {:rel "stylesheet"
            :href "/css/style.css"}]]
   [:body
    [:div.container content]
    [:script {:src "http://code.jquery.com/jquery.js"}]
    [:script {:src "//netdna.bootstrapcdn.com/bootstrap/3.0.0-rc1/js/bootstrap.min.js"}]
    [:script {:src "/js/codemirror.js"}]
    [:script {:src "/js/mode/clojure/clojure.js"}]
    [:script {:src "/js/mode/css/css.js"}]
    [:script {:src "/js/app.js"}]
    [:script "cljsfiddle.core.init();"]]])

(defn main-view 
  [fiddle] 
  (base [:hr]
        [:div.row
         [:div.col-lg-12
          [:button#run-btn.btn.btn-primary {:type "Button"} "Run"] " "
          [:button#save-btn.btn.btn-default {:type "Button"} "Save"] " "
          [:button#recent-btn.btn.btn-default {:type "Button"} "Recent"] " "
          [:button#about-btn.btn.btn-default {:type "Button"} "About"] " "
          [:button#about-btn.btn.btn-default {:type "Button"} "Help"]]]
        [:hr]
        [:div.row
         [:div.col-lg-6 [:textarea#html-editor (escape-html (:html fiddle))]]
         [:div.col-lg-6 [:textarea#css-editor (escape-html (:css fiddle))]]]
        [:div.row
         [:div.col-lg-6 [:textarea#cljs-editor (escape-html (:cljs fiddle))]]
         [:div.col-lg-6 [:iframe#result-frame {:seamless "seamless"
                                               :sandbox "allow-scripts"
                                               :width "100%"
                                               :style "border: 1px solid black;height:260px;"}]]]
        [:hr]
        [:div.row
         [:div.col-lg-12
          [:p.text-center {:style "margin-bottom: 10px;"} [:a {:href "http://www.cljsfiddle.net"} "cljsfiddle"] " &copy; 2013 Jonas Enlund"]]]))
