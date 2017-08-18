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

  :cljsbuild {:builds {:dev {:figwheel {:on-jsload "book-sorter.core/run"}
                             :source-paths ["src/cljs" "src/cljc"]
                             :compiler {:main "book-sorter.core"
                                        :asset-path "/js"
                                        :optimizations :none
                                        :source-map true
                                        :source-map-timestamp true
                                        :output-dir "resources/public/js"
                                        :output-to "resources/public/js/main.js"}}
                       :prod {:source-paths ["src/cljs" "src/cljc"]
                              :compiler {:optimizations :advanced
                                         :elide-asserts true
                                         :pretty-print false
                                         :output-to "resources/public/js/main.js"}}
                       :test {:source-paths ["src/cljs" "src/cljc" "test-cljs"]
                              :compiler {:optimizations :whitespace
                                         :output-dir "resources/private/js"
                                         :output-to "resources/private/js/unit-test.js"}}}
              
              :test-commands
              {"unit" ["phantomjs"
                       "phantom/unit-test.js"
                       "resources/private/unit-test.html"]}}
  
  :clean-targets ^{:protect false} ["resources/public/js" "resources/private/js"]
  
  :ring {:handler book-sorter.routes/app}
  :figwheel {:repl false
             :ring-handler book-sorter.routes/dev-app})
