(ns cljsfiddle.handler
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [cljsfiddle.views :as views]
            [cljsfiddle.closure :as closure]
            [cljsfiddle.db :as db]
            [datomic.api :as d]
            [ring.adapter.jetty :as jetty]
            [ring.util.response :as res]
            [ring.middleware.session.cookie :as cookie]
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
                 {:form-params {:client_id (env :github-client-id)
                                :client_secret (env :github-client-secret)
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

(def default-fiddle 
  {:fiddle/css "p {\n  color: #f41;\n  font-family: helvetica;\n  font-size: 2em;\n  text-align: center\n}"
   :fiddle/html "<p>Hello, <strong id=\"msg\"></strong></p>\n"
   :fiddle/cljs "(ns cljsfiddle\n  (:require [domina :as dom]))\n\n(dom/set-html! (dom/by-id \"msg\") \"ClojureScript\")\n"})

(defn app-routes [conn]
  (routes
   (GET "/"
     {{:keys [username] :as session} :session}
     (assoc {:headers {"Content-Type" "text/html"}
             :status 200
             :body (html5 (views/main-view default-fiddle username))}
       :session (dissoc session :fiddle)))
   
   (GET "/fiddle/:ns"
     {{:keys [username] :as session} :session
      {:keys [ns]} :params}
     (when-let [fiddle (db/find-by-ns (d/db conn) ns)]
       (assoc {:headers {"Content-Type" "text/html"}
               :status 200
               :body (html5 (views/main-view fiddle username))}
         :session (merge session
                         {:fiddle ns}))))


   (POST "/save"
     [data]
     (let [fiddle (edn/read-string data)
           ns (second (edn/read-string (:fiddle/cljs fiddle)))]
       (db/upsert conn (assoc fiddle :fiddle/ns (name ns))) 
       (edn-response {:status :success})))
   
   (GET "/oauth_login"
     {{:keys [code]} :params session :session}
     (if code
       (let [token (get-token code)
             _ (println token)
             username (get-username token)
             _ (println username)]
         (assoc (res/redirect (if-let [fiddle (:fiddle session)]
                                (str "/fiddle/" fiddle)
                                "/"))
           :session (merge session {:token token 
                                    :username username})))))

   (GET "/logout"
     []
     (assoc (res/redirect "/") :session nil))
   
   (context "/compiler" [] closure/compiler-routes)
   
   (route/resources "/")
   (route/not-found "Not Found")))

(defn app [conn store]
  (handler/site (app-routes conn) 
                {:session {:store store}}))

(defn -main []
  (let [port (Integer/parseInt (or (env "PORT") "8080"))
        store (cookie/cookie-store {:key (env :session-secret)})
        db-uri "datomic:mem://cljsfiddle"
        conn (do (d/delete-database db-uri)
                 (d/create-database db-uri)
                 (d/connect db-uri))]
    (db/create conn db/schema)
    (jetty/run-jetty (app conn store) {:port port :join? false})))

;; (.stop server)
;; (def server (-main))