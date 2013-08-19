(ns cljsfiddle.handler
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [cljsfiddle.views :as views]
            [cljsfiddle.closure :as closure]
            [cljsfiddle.db :as db]
            [datomic.api :as d]
            [ring.adapter.jetty :as jetty]
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

;; 1 day(s)
(def max-age (str "max-age=" (* 60 60 24 1)))

(defn app-routes [conn]
  (routes
   (GET "/"
     [] 
     (html5 (views/main-view {:fiddle/css "p {\n  color: #f41;\n  font-family: helvetica;\n  font-size: 2em;\n  text-align: center\n}"
                              :fiddle/html "<p>Hello, <strong id=\"msg\"></strong></p>\n"
                              :fiddle/cljs "(ns cljsfiddle\n  (:require [domina :as dom]))\n\n(dom/set-html! (dom/by-id \"msg\") \"ClojureScript\")\n"})))
   
   (GET "/fiddle/:ns"
     [ns]
     (html5
      (if-let [fiddle (db/find-by-ns (d/db conn) ns)]
        (views/main-view fiddle)
        (views/main-view {:fiddle/css ""
                          :fiddle/html (format "<p>No such namespace: <strong>%s</strong></p>\n" ns)
                          :fiddle/cljs (format "(ns %s)\n" ns)}))))
   
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
           ns (second (edn/read-string (:fiddle/cljs fiddle)))]
       (db/upsert conn (assoc fiddle :fiddle/ns (name ns))) 
       (edn-response {:status :success})))
   
   (GET "/api/fiddle/:ns"
     [ns]
     (if-let [res (db/find-by-ns ns)]
       (edn-response res)
       (edn-response {:status :fail
                      :msg "No such namespace"
                      :ns ns})))
   
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
   (route/not-found "Not Found")))

(defn app [conn]
  (handler/site (app-routes conn)))

(defn -main []
  (let [port (Integer/parseInt (or (env "PORT") "8080"))
        db-uri "datomic:mem://cljsfiddle"
        conn (do (d/delete-database db-uri)
                 (d/create-database db-uri)
                 (d/connect db-uri))]
    (db/create conn db/schema)
    (jetty/run-jetty (app conn) {:port port :join? false})))

;; (.stop server)
;; (def server (-main))