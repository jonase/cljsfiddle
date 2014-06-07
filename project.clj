(defproject cljsfiddle "0.1.0-SNAPSHOT"
  :description "CLJSFiddle"
  :url "http://cljsfiddle.net"
  :dependencies [[org.clojure/clojure "1.6.0"]
                 #_[org.clojure/clojurescript "0.0-1853"]
                 #_[org.clojure/clojurescript "0.0-1896"]
                 [org.clojure/clojurescript "0.0-2227"]
                 [org.clojure/tools.reader "0.8.4"]
                 [org.clojure/core.match "0.2.1"]
                 [org.clojure/core.async "0.1.303.0-886421-alpha"]
                 [org.clojure/core.logic "0.8.7"]
                 [org.clojure/tools.macro "0.1.5"]
                 [com.datomic/datomic-free "0.9.4815"]
                 [ring/ring-jetty-adapter "1.3.0"]
                 [ring/ring-devel "1.3.0"]
                 [fogus/ring-edn "0.2.0"]
                 [commons-codec "1.9"]
                 [me.raynes/fs "1.4.5"]
                 [compojure "1.1.8"]
                 [clj-http "0.9.2"]
                 [cheshire "5.3.1"]
                 [hiccup "1.0.5"]
                 [environ "0.5.0"]
                 [com.taoensso/timbre "3.2.1"]
                 [hylla "0.2.0"]
                 [domina "1.0.2"]
                 [prismatic/dommy "0.1.2"]
                 [hiccups "0.3.0"]
                 [cljs-ajax "0.2.4"]]
  :source-paths ["src/clj" "src/cljs"]
  :plugins [[lein-ring "0.8.10"]
            [lein-cljsbuild "1.0.3"]]
;  :main cljsfiddle.handler
;  :uberjar-name "cljsfiddle-standalone.jar"
  :min-lein-version "2.0.0"
  :profiles {:dev {:dependencies [[ring-mock "0.1.5"]]}}
  :ring {:handler cljsfiddle.handler/app
         :port 8080
         :stacktraces? true
         :auto-reload? true}
  :cljsbuild {:builds {:dev {
                        :source-paths ["src/cljs"]
                        :compiler {:output-to "resources/public/js/app.js"
                                   :output-dir "resources/public/js/out-dev"
                                   :source-map true
                                   :optimizations :simple
                                   :pretty-print true}}
                       :prod {
                        :source-paths ["src/cljs"]
                        :compiler {:output-to "resources/public/js/app.js"
                                   :output-dir "resources/public/js/out"
                                   :optimizations :advanced
                                   :source-map "resources/public/js/app.js.map"
                                   :pretty-print false
                                   :elide-asserts true
                                   :static-fns true
                                   :externs ["externs.js"]}}}}
  :aliases {"db-create" ["trampoline" "run" "-m" "cljsfiddle.import/create-db"
                          "datomic:free://localhost:4334/cljsfiddle"]
            "db-assets" ["trampoline" "run" "-m" "cljsfiddle.import"
                          "datomic:free://localhost:4334/cljsfiddle"]})
