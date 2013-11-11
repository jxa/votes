(ns voter.tasks
  (:require [net.cgrand.enlive-html :as html]
            [me.raynes.fs :as fs]
            [clojure.string :as cstring])
  )

;; Thanks http://stackoverflow.com/questions/14409014/piping-a-subprocesses-output-directly-to-stdout-java-clojure

(defn- print-return-stream
    [stream]
    (let [stream-seq (->> stream
                          (java.io.InputStreamReader.)
                          (java.io.BufferedReader.)
                          line-seq)]
        (doall (reduce
            (fn [acc line]
                (println line)
                (if (empty? acc) line (str acc "\n" line)))
            ""
            stream-seq))))

(defn exec-stream
    "Executes a command in the given dir, streaming stdout and stderr to stdout,
    and once the exec is finished returns a vector of the return code, a string of

    all the stdout output, and a string of all the stderr output"
    [dir command & args]
    (let [runtime  (Runtime/getRuntime)
          proc     (.exec runtime (into-array (cons command args)) nil (java.io.File. dir))
          stdout   (.getInputStream proc)
          stderr   (.getErrorStream proc)
          outfut   (future (print-return-stream stdout))
          errfut   (future (print-return-stream stderr))
          proc-ret (.waitFor proc)]
        [proc-ret @outfut @errfut]))

(defn pwd []
  (System/getProperty "user.dir"))


(defn- get-mtimes [paths]
  (into {}
    (map (fn [path] [path (fs/mod-time path)]) paths)))

(defn dist
  "distribution path"
  ([env] (str "dist/" env))
  ([env path] (str "dist/" env "/" path)))

(defn watch []
  (let [group (ThreadGroup. "file watchers")
        sass-watch (fn [] (exec-stream (pwd) "sass" "--watch" "src/sass:resources/public/css"))
        sass-thread (Thread. group sass-watch)
        file-watch (fn [] )]
    ;; should I cleaan up the threads in a (Runtime/addShutdownHook ) ?
    (.start sass-thread)
    (.wait group)))

(defn asset-url [env path]
  (str (if (= env "development")
         "http://localhost:3000/"
         (str "https://dl.dropboxusercontent.com/u/108659/estimation_party/" env "/"))
       path))

(html/deftemplate page "page.html"
  [env]
  [:head :link] (html/set-attr :href (asset-url env "css/app.css"))
  [:script#js-app] (html/set-attr :src (asset-url env "js/app.js"))
  [:script#js-initialize] (html/content
                           (if (= env "development")
                             "window.__include_brepl = true;"
                             "voter.core.run();")))

(html/deftemplate manifest "manifest.xml"
  [env content]
  [:Content] (html/content content))

(defn build-development-html [page]
  (spit (fs/file "resources/public/test.html") (cstring/join page)))

(defn build-manifest [page env]
  (let [dest (dist env "manifest.xml")
        content (cstring/join (manifest env (cstring/join page)))]
    (spit (fs/file dest) content)))

(defn build-html [env]
  (if (= env "development")
    (build-development-html (page env))
    (build-manifest (page env) env)))

(defn build-css [env]
  (let [[_ css _] (exec-stream (pwd) "sass" "src/sass/app.scss")]
    (fs/mkdir (dist env "css"))
    (spit (fs/file (dist env "css/app.css")) css)))

;; TODO: copy production and staging js from a build dir
;;  the problem is that cljsbuild clean wipes out ALL files from ALL builds
(defn build
  "build static assets for specified environment"
  [& [env]]
  (build-html env)
  (build-css env)
  (System/exit 0))
