(defproject cljsfiddle "0.1.0-SNAPSHOT"
  :description "CLJSFiddle"
  :url "http://cljsfiddle.net"
  :dependencies [[org.clojure/clojure "1.5.1"]
                 #_[org.clojure/clojurescript "0.0-1853"]
                 #_[org.clojure/clojurescript "0.0-1896"]
                 [org.clojure/clojurescript "0.0-1909"] 
                 [org.clojure/tools.reader "0.7.8"]
                 [org.clojure/core.match "0.2.0"]
                 [org.clojure/core.async "0.1.242.0-44b1e3-alpha"]
                 [org.clojure/core.logic "0.8.4"]
                 [com.datomic/datomic-free "0.8.4138"]
                 [ring/ring-jetty-adapter "1.2.0"]
                 [ring/ring-devel "1.2.0"]
                 [fogus/ring-edn "0.2.0"]
                 [commons-codec "1.7"]
                 [me.raynes/fs "1.4.5"]
                 [compojure "1.1.5"]
                 [clj-http "0.7.6"]
                 [cheshire "5.2.0"]
                 [hiccup "1.0.4"]
                 [environ "0.4.0"]
                 [hylla "0.1.0"]
                 [domina "1.0.1"]
                 [prismatic/dommy "0.1.2"]
                 [hiccups "0.2.0"]
                 [cljs-ajax "0.2.0"]]
  :source-paths ["src/clj" "src/cljs"]
  :plugins [[lein-ring "0.8.6"]
            [lein-cljsbuild "0.3.3"]]
;  :main cljsfiddle.handler
;  :uberjar-name "cljsfiddle-standalone.jar"
  :min-lein-version "2.0.0"
  :profiles {:dev {:dependencies [[ring-mock "0.1.5"]]}}
  :ring {:handler cljsfiddle.handler/app}
  :cljsbuild {:builds [{:id "dev"
                        :source-paths ["src/cljs"]
                        :compiler {:output-to "resources/public/js/app.js"  
                                   :optimizations :simple
                                   :pretty-print true}}
                       {:id "prod"
                        :source-paths ["src/cljs"]
                        :compiler {:output-to "resources/public/js/app.js"  
                                   :optimizations :advanced
                                   :static-fns true
                                   :externs ["externs.js"]}}]})
