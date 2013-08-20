(ns cljsfiddle.views
  (:require [hiccup.util :refer (escape-html)]
            [environ.core :refer (env)]))

(defn base [& content] 
  [:html {:lang "en"}
   [:head
    [:title "CLJSFiddle"]
    [:meta {:name "viewport"
            :content "width=device-width, initial-scale=1.0"}]
    [:link {:rel "stylesheet"
            :href "//netdna.bootstrapcdn.com/bootstrap/3.0.0/css/bootstrap.min.css"}]
    [:link {:rel "stylesheet"
            :href "/css/codemirror.css"}]
    [:link {:rel "stylesheet"
            :href "/css/style.css"}]]
   [:body
    [:div.container content]
    [:script {:src "http://code.jquery.com/jquery.js"}]
    [:script {:src "//netdna.bootstrapcdn.com/bootstrap/3.0.0/js/bootstrap.min.js"}]
    [:script {:src "/js/codemirror.js"}]
    [:script {:src "/js/mode/clojure/clojure.js"}]
    [:script {:src "/js/mode/css/css.js"}]
    [:script {:src "/js/app.js"}]
    [:script "cljsfiddle.core.init();"]]])

(def ^:private github-login-url (str "https://github.com/login/oauth/authorize?client_id=" (env :github-client-id)))

(defn main-view 
  [fiddle user] 
  (base [:hr {:style "margin-top:4px;margin-bottom:4px;"}]
        [:div.row
         [:div.col-lg-12
          [:button#run-btn.btn.btn-default {:type "Button"} "Run"] " "
          [:button#save-btn.btn.btn-default {:type "Button"} "Save"] " "
          [:button#recent-btn.btn.btn-default {:type "Button"} "Recent"] " "
          [:button#about-btn.btn.btn-default {:type "Button"} "About"] " "
          [:button#about-btn.btn.btn-default {:type "Button"} "Help"]
          [:span.pull-right (if user
                              [:a {:href "/logout"} "Logout (" user ")"]
                              [:a {:href github-login-url} 
                               "Login with github"])]]]
        [:hr {:style "margin-top:4px;margin-bottom:4px;"}]
        [:div.row
         [:div.col-lg-6 [:textarea#html-editor (escape-html (:fiddle/html fiddle))]]
         [:div.col-lg-6 [:textarea#css-editor (escape-html (:fiddle/css fiddle))]]]
        [:div.row
         [:div.col-lg-6 [:textarea#cljs-editor (escape-html (:fiddle/cljs fiddle))]]
         [:div.col-lg-6 [:iframe#result-frame {:seamless "seamless"
                                               :sandbox "allow-scripts"
                                               :width "100%"
                                               :style "border: 1px solid lightgray;height:260px;"}]]]
        [:hr]
        [:div.row
         [:div.col-lg-12
          [:p.text-center {:style "margin-bottom: 10px;"} 
           [:a {:href "http://www.cljsfiddle.net"} "cljsfiddle"] " &copy; 2013 Jonas Enlund"]]]))
