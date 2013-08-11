(ns cljsfiddle.db
  (:refer-clojure :exclude (drop))
  (:require [clojure.java.jdbc :as sql]
            [environ.core :refer (env)]))

(defn table-exists? [tablename]
  (not (empty? (sql/with-connection (env :heroku-postgresql-ivory-url)
                 (sql/with-query-results results
                   ["SELECT * FROM pg_tables WHERE tablename=?" tablename]
                   (into [] results))))))

(defn create []
  (when-not (table-exists? "fiddles")
    (sql/with-connection (env :heroku-postgresql-ivory-url)
      (sql/create-table :fiddles
                        [:ns :varchar "PRIMARY KEY"]
                        [:cljs :text "NOT NULL"]
                        [:html :text]
                        [:css :text]
                        [:created :timestamp "NOT NULL" "DEFAULT CURRENT_TIMESTAMP"]))))

(defn drop []
  (sql/with-connection (env :heroku-postgresql-ivory-url)
    (sql/drop-table :fiddles)))

(defn upsert [fiddle]
  (let [ns (:ns fiddle)]
    (sql/with-connection (env :heroku-postgresql-ivory-url)
      (sql/update-or-insert-values :fiddles ["ns = ?" ns] fiddle))))

(defn find-by-ns [ns]
  (first
   (sql/with-connection (env :heroku-postgresql-ivory-url)
     (sql/with-query-results results
       ["SELECT * FROM fiddles WHERE ns = ?" ns]
       (into [] results)))))
