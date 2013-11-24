(ns voter.tasks
  (:require [net.cgrand.enlive-html :as html]
            [me.raynes.fs :as fs]
            [clojure.string :as cstring]
            [clojure.java.shell :refer [sh]]
            [clojure.data.xml :as xml]
            [clojure.java.io :as io]))


(defn dist
  "distribution path"
  ([env] (str "dist/" env))
  ([env path] (str "dist/" env "/" path)))

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

(defn build-development-html
  "Output html directly for use in local dev"
  [page]
  (spit (fs/file "resources/public/test.html") (cstring/join page)))

(defn build-manifest
  "Wrap the html content in the XML that google expects"
  [content env]
  (with-open [out (io/writer (fs/file (dist env "manifest.xml")))]
    (xml/emit
     (xml/sexp-as-element
      [:Module
       [:ModulePrefs {:title "Estimation Party"}
        [:Require {:feature "rpc"}]
        [:Require {:feature "views"}]
        [:Require {:feature "locked-domain"}]]
       [:Content {:type "html"}
        [:-cdata content]]])
     out)))

(defn build-html [env]
  (if (= env "development")
    (build-development-html (page env))
    (build-manifest (cstring/join (page env)) env)))

(defn build-css [env]
  (let [{:keys [out exit err]} (sh "sass" "src/sass/app.scss")]
    (if (= 0 exit)
      (do
        (fs/mkdir (dist env "css"))
        (spit (fs/file (dist env "css/app.css")) out))
      (throw (Error. err)))))

(defn build-js [env]
  (fs/mkdir (dist env "js"))
  (fs/copy "target/cljs/app.js" (dist env "js/app.js"))
  (fs/copy "target/cljs/app.js.map" (dist env "js/app.js.map")))

(defn build
  "build static assets for specified environment"
  [& [env]]
  (build-html env)
  (build-css env)
  (build-js env)
  (System/exit 0))
