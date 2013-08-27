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
  (base [:nav.navbar.navbar-default {:role "navigation"}
         [:div.navbar-header
          [:button.navbar-toggle {:type "button"
                                  :data-toggle "collapse"
                                  :data-target ".navbar-ex1-collapse"}
           [:span.sr-only "Toggle navigation"]
           [:span.icon-bar]
           [:span.icon-bar]
           [:span.icon-bar]]
          [:a.navbar-brand "CLJSFiddle"]]
         [:div.collapse.navbar-collapse.navbar-ex1-collapse
          [:ul.nav.navbar-nav
           [:li [:button#run-btn.btn.btn-default.navbar-btn {:type "button"} "Run"] "&nbsp;"]
           [:li [:button#save-btn.btn.btn-default.navbar-btn {:type "button"} "Save"] "&nbsp;"]
           [:li [:button#about-btn.btn.btn-default.navbar-btn {:type "button"} "About"]]]
          [:ul.nav.navbar-nav.navbar-right
           [:li (if user 
                  [:a {:href "/logout"} "Logout (" user ")"] 
                  [:a {:href github-login-url} "Login"])]]]]
        [:div.row
         [:div.col-lg-12
          [:div#alert]]]
        [:div.row
         [:div.col-lg-6 [:ul.nav.nav-tabs
                         [:li.active [:a {:href "#cljs-editor-tab" :data-toggle "tab"} "cljs"]]
                         [:li [:a {:href "#html-editor-tab" :data-toggle "tab"} "html"]]
                         [:li [:a {:href "#css-editor-tab" :data-toggle "tab"} "css"]]] 
          [:div.tab-content
           [:div#cljs-editor-tab.tab-pane.active
            [:textarea#cljs-editor.tab-pane.active (escape-html (:fiddle/cljs fiddle))]] 
           [:div#html-editor-tab.tab-pane
            [:textarea#html-editor.tab-pane (escape-html (:fiddle/html fiddle))]]
           [:div#css-editor-tab.tab-pane
            [:textarea#css-editor.tab-pane (escape-html (:fiddle/css fiddle))]]]]
         [:div.col-lg-6 [:iframe#result-frame {:seamless "seamless"
                                               :sandbox "allow-scripts"
                                               :width "100%"
                                               :style "border: 1px solid lightgray;height:500px;"}]]]
        [:hr]
        [:div.row
         [:div.col-lg-12
          [:p.text-center {:style "margin-bottom: 10px;"} 
           [:a {:href "http://cljsfiddle.net"} "cljsfiddle.net"] " &copy; 2013 Jonas Enlund"]]]))
