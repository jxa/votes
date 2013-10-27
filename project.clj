(defproject voter "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/clojurescript "0.0-1934"]
                 [ring "1.2.0"]
                 [prismatic/dommy "0.1.1"]
                 [org.clojure/core.async "0.1.242.0-44b1e3-alpha"]
                 [com.cemerick/piggieback "0.1.0"]]

  :plugins [[lein-cljsbuild "0.3.4"]
            [lein-ring "0.8.7"]]
  :hooks [leiningen.cljsbuild]
  :source-paths ["src/clj"]
  :main voter.server
  :ring {:handler voter.server/app}

  :repl-options {:nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}
  :injections [(require '[cljs.repl.browser :as browser]
                        '[cemerick.piggieback :as pb])
               (defn brepl
                 ([] (brepl 9000))
                 ([port] (pb/cljs-repl :repl-env (browser/repl-env :port port))))]

  :cljsbuild {:builds {:prod {:source-paths ["src/cljs"]
                              :jar true
                              :compiler {:output-to "resources/public/js/app.js"
                                         :source-map "resources/public/js/app.js.map"
                                         :optimizations :advanced
                                         :pretty-print false
                                         :externs ["resources/externs.js"]}}
                       :dev {:source-paths ["src/cljs"]
                             :jar true
                             :compiler {:output-to "resources/public/js/app.dev.js"
                                        :optimizations :whitespace
                                        :pretty-print true
                                        :externs ["resources/externs.js"]}}}})
