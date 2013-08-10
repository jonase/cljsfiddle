(ns cljsfiddle.handler
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [cljsfiddle.views :as views]
            [cljsfiddle.closure :as closure]
            [cljsfiddle.db :as db]
            [compojure.core :refer :all] 
            [compojure.handler :as handler]
            [compojure.route :as route]
            [environ.core :refer (env)]
            [clj-http.client :as http]
            [cheshire.core :as json]
            [hiccup.page :refer [html5]]))

(defn edn-response [edn-data]
  {:status 200
   :headers {"Content-Type" "application/edn"}
   :body (pr-str edn-data)})

;; From technomancy/syme
(defn get-token [code]
  (-> (http/post "https://github.com/login/oauth/access_token"
                 {:form-params {:client_id (env :oauth-client-id)
                                       :client_secret (env :oauth-client-secret)
                                       :code code}
                  :headers {"Accept" "application/json"}})
      (:body) 
      (json/decode true) 
      :access_token))

(defn get-username [token]
  (-> (http/get (str "https://api.github.com/user?access_token=" token)
                {:headers {"accept" "application/json"}})
      (:body) 
      (json/decode true) 
      :login))

;; 7 days
(def max-age (str "max-age=" (* 60 60 24 7)))

(defroutes app-routes
  (GET "/" 
    [] 
    (html5 (views/main-view {:css "p {\n  color: #f41;\n  font-family: helvetica;\n  font-size: 2em;\n  text-align: center\n}"
                             :html "<p>Hello, <strong id=\"msg\"></strong></p>\n"
                             :cljs "(ns cljsfiddle\n  (:require [domina :as dom]))\n\n(dom/set-html! (dom/by-id \"msg\") \"ClojureScript\")\n"})))
  
  (GET "/fiddle/:ns"
    [ns]
    (let [fiddle (db/find-by-ns ns)]
      (html5
       (if fiddle
         (views/main-view fiddle)
         (views/main-view {:css "" 
                          :html (format "<p>No such namespace: <strong>%s</strong></p>\n" ns)
                          :cljs (format "(ns %s)\n" ns)})))))
  
  ;; TODO /api
  (POST "/compile"
    [data]
    ;; TODO :src -> :cljs
    (let [cljs-src-str (:src (edn/read-string data))]
      (edn-response (closure/compile-cljs cljs-src-str))))
  
  ;; TODO /api
  (POST "/save"
    [data]
    (let [fiddle (edn/read-string data)
          ns (second (edn/read-string (:cljs fiddle)))]
      (db/upsert (assoc fiddle :ns (name ns))) 
      (edn-response {:status :success})))
  
  (GET "/api/fiddle/:ns"
    [ns]
    (if-let [res (db/find-by-ns ns)]
      (edn-response res)
      (edn-response {:status :fail
                     :msg "No such namespace"
                     :ns ns})))
  
  (GET "deps.js"
    [version]
    {:headers {"Cache-Control" max-age
               "Content-Type" "text/javascript"} 
     :status 200
     :body ""})
  
  (GET "/deps/:version/goog.base"
    [version]
    {:headers {"Cache-Control" max-age
               "Content-Type" "text/javascript"} 
     :status 200
     :body (:js-src (closure/closure-compile 
                     "goog/base.js" 
                     (slurp (io/resource "goog/base.js"))))})

  (GET "/deps/:version/*"
    [version *]
    (let [file *]
      {:headers {"Cache-Control" max-age
                 "Content-Type" "text/javascript"} 
       :status 200
       :body (:js-src (closure/compile-file file))}))
  
  (route/resources "/")
  (route/not-found "Not Found"))

(def app
  (handler/site app-routes))
