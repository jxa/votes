(defproject voter "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/clojurescript "0.0-1978"]
                 [ring "1.2.0"]
                 [com.keminglabs/c2 "0.2.3"]
                 [com.cemerick/piggieback "0.1.0"]]

  :plugins [[lein-cljsbuild "0.3.4"]
            [lein-ring "0.8.7"]]
  :hooks [leiningen.cljsbuild]
  :source-paths ["src/clj"]
  :ring {:handler voter.server/app}

  ;; :profiles {:dev {:plugins [[com.cemerick/clojurescript.test "0.1.0"]]}}

  :repl-options {:nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}

  :cljsbuild {:builds
              {;; :prod {:source-paths ["src/cljs"]
               ;;        :jar true
               ;;        :compiler {:output-to "resources/public/js/app.js"
               ;;                   :source-map "resources/public/js/app.js.map"
               ;;                   :optimizations :advanced
               ;;                   :pretty-print false
               ;;                   :externs ["resources/externs.js"]}}
               :dev {:source-paths ["src/cljs"]
                     :jar true
                     :compiler {:output-to "resources/public/js/app.dev.js"
                                :optimizations :whitespace
                                :pretty-print true
                                :externs ["resources/externs.js"]}}
              ;;  :test {:source-paths ["src/cljs" "test/cljs"]
              ;;         :jar true
              ;;         :compiler {:output-to "target/cljs/test.js"
              ;;                    :optimizations :whitespace
              ;;                    :pretty-print true
              ;;                    :externs ["resources/externs.js"]}}
              ;;  }
              ;; :test-commands {"unit" ["phantomjs" :runner "target/cljs/test.js"]
               }}

  :injections [(require '[cljs.repl.browser :as browser]
                        '[cemerick.piggieback :as pb])
               (defn brepl
                 ([] (brepl 9000))
                 ([port] (pb/cljs-repl :repl-env (browser/repl-env :port port))))])
