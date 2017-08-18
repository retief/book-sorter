(defproject book-sorter "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  
  :source-paths ["src/clj" "src/cljc"]
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.9.854"
                  :exclusions [org.apache.ant/ant]]
                 [javax.servlet/servlet-api "2.5"]
                 [reagent  "0.7.0"]
                 [re-frame "0.9.4"]
                 [day8.re-frame/http-fx "0.1.4"]
                 [ring/ring-devel "1.6.2"]
                 [ring/ring-mock "0.3.1"]
                 [cheshire "5.8.0"]
                 [bidi "2.1.2"]
                 [kibu/pushy "0.3.7"]]
  :plugins [[lein-cljsbuild "1.1.7"]
            [lein-ring "0.12.0"]
            [lein-figwheel  "0.5.4-7"]]

  :profiles {:dev {:cljsbuild
                   {:builds {:client {:figwheel     {:on-jsload "book-sorter.core/run"}
                                      :compiler     {:main "book-sorter.core"
                                                     :asset-path "js"
                                                     :optimizations :none
                                                     :source-map true
                                                     :source-map-timestamp true}}}}}

             :prod {:cljsbuild
                    {:builds {:client {:compiler    {:optimizations :advanced
                                                     :elide-asserts true
                                                     :pretty-print false}}}}}}
  
  :cljsbuild {:builds {:client {:source-paths ["src/cljs" "src/cljc"]
                                :compiler {:output-dir "resources/public/js"
                                           :output-to "resources/public/js/main.js"}}}}
  
  :clean-targets ^{:protect false} ["resources/public/js"]
  
  :ring {:handler book-sorter.routes/app}
  :figwheel {:repl false
             :ring-handler book-sorter.routes/dev-app})
