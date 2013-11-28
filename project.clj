(defproject voter "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/clojurescript "0.0-2080"]
                 [ring "1.2.1"]
                 [com.keminglabs/c2 "0.2.4-SNAPSHOT"]
                 [com.cemerick/piggieback "0.1.2"]
                 [me.raynes/fs "1.4.5"]
                 [enlive "1.1.4"]
                 [org.clojure/data.xml "0.0.7"]
                 [com.cemerick/clojurescript.test "0.2.1"]]

  :plugins [[lein-cljsbuild "1.0.0"]
            [lein-ring "0.8.7"]
            [com.cemerick/clojurescript.test "0.2.1"]]
  :hooks [leiningen.cljsbuild]

  :source-paths ["src/clj"]
  :resource-paths ["resources" "src/templates"]

  :ring {:handler voter.server/app}

  :aliases {"distribute" ["run" "-m" "voter.tasks/distribute"]}

  :repl-options {:nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}

  :cljsbuild {:builds {:dev {:source-paths ["src/cljs"]
                             :compiler {:output-to "resources/public/js/app.js"
                                        :optimizations :whitespace
                                        :pretty-print true
                                        :externs ["resources/externs.js"]}}
                       :test {:source-paths ["src/cljs" "test/cljs"]
                              :compiler {:output-to "target/cljs/test.js"
                                         :optimizations :whitespace
                                         :pretty-print true
                                         :externs ["resources/externs.js"]}}
                       :prod {:source-paths ["src/cljs"]
                              :compiler {:output-to "target/cljs/app.js"
                                         :optimizations :advanced
                                         :pretty-print false
                                         :externs ["resources/externs.js"]}}}
              :test-commands {"unit-tests" ["phantomjs" :runner "target/cljs/test.js"]}}

  :injections [(require '[cljs.repl.browser :as browser]
                        '[cemerick.piggieback :as pb])
               (defn brepl
                 ([] (brepl 9000))
                 ([port] (pb/cljs-repl :repl-env (browser/repl-env :port port))))])
