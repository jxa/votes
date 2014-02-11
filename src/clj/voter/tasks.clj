(ns voter.tasks
  (:require [net.cgrand.enlive-html :as html]
            [me.raynes.fs :as fs]
            [clojure.string :as cstring]
            [clojure.java.shell :refer [sh]]
            [clojure.data.xml :as xml]
            [clojure.java.io :as io])
  (:import [java.security MessageDigest]))

(defn md5 [string]
  (let [bytes (.getBytes (str string) "UTF-8")
        digest (.digest (MessageDigest/getInstance "MD5") bytes)]
    (format "%032x" (BigInteger. 1 digest))))

(defn add-md5 [content path]
  (if-let [[match fname ext] (re-matches #"^(.+)\.(\w+)$" path)]
    (str fname "-" (md5 content) "." ext)
    (str path "-" (md5 content))))

(defn write-with-md5
  "computes MD5 hash, appends it to the filename, writes content to that filename
   and returns the filename"
  [content path]
  (let [md5path (add-md5 content path)]
    (spit md5path content)
    md5path))

(defn bucket [context]
  (case (:env context)
    "staging" "estimationparty-staging"
    "production" "estimationparty.com"))

(defn s3-path [context file]
  (str "//" (bucket context)
       ".s3.amazonaws.com"
       (cstring/replace file (:path context) "")))

(html/deftemplate page "page.html"
  [{:keys [env path js css] :as context}]
  [:head :link] (html/set-attr :href (s3-path context css))
  [:script#js-app] (html/set-attr :src (s3-path context js))
  [:script#js-initialize] (html/content "voter.core.run(gapi.hangout);"))

(defn build-manifest
  "Wrap the html content in the XML that google expects"
  [{:keys [path] :as context}]
  (println "building xml manifest")
  (let [content (cstring/join (page context))
        manpath (str path "/manifest.xml")]
    (with-open [out (io/writer (fs/file manpath))]
      (xml/emit
       (xml/sexp-as-element
        [:Module
         [:ModulePrefs {:title "Estimation Party"}
          [:Require {:feature "rpc"}]
          [:Require {:feature "views"}]
          [:Require {:feature "locked-domain"}]]
         [:Content {:type "html"}
          [:-cdata content]]])
       out))
    (assoc context :manifest manpath)))

(defn copy-static-files [{:keys [env path] :as context}]
  (println "copying static files")
  (let [{:keys [out exit err]} (sh "cp" "-R" "resources/static/" path)]
    (when-not (zero? exit)
      (throw (ex-info "Static file copy error" {:stderr err :stdout out}))))
  context)

(defn build-js [{:keys [env path] :as context}]
  (println "copying JS")
  (fs/mkdir (str path "/js"))
  (assoc context :js (write-with-md5 (slurp "target/cljs/app.js") (str path "/js/app.js"))))

(defn build-css [{:keys [env path] :as context}]
  (println "building CSS")
  (let [{:keys [out exit err]} (sh "sass" "src/sass/app.scss")]
    (when-not (zero? exit)
      (throw (ex-info "Build CSS error" {:stderr err :stdout out})))
    (fs/mkdir (str path "/css"))
    (assoc context :css (write-with-md5 out (str path "/css/app.css")))))

(defn sync-s3 [{:keys [env path] :as context}]
  (println "Copying files to S3")
  (let [{:keys [out exit err]} (sh "aws" "s3" "sync" path
                                   (str "s3://" (bucket context))
                                   "--delete" "--acl=public-read")]
    (when-not (zero? exit)
      (throw (ex-info
              "Do you have the aws command line utils installed?\nhttp://aws.amazon.com/cli/"
              {:stderr err :stdout out})))
    (println out)
    (assoc context :bucket bucket)))

(defn ensure-env [{:keys [env] :as context}]
  (if (contains? #{"production" "staging"} env)
    (do
      (fs/mkdir (str "dist/" env))
      (assoc context :path (str "dist/" env)))
    (do
      (println "No such environment" env)
      (System/exit 0))))

(defn distribute
  "css-gen -> css-write -> js-gen -> js-write -> html-gen -> html-write"
  [& [env]]
  (if env
    (-> {:env env}
        ensure-env
        copy-static-files
        build-css
        build-js
        build-manifest
        sync-s3
        println)
    (println "Usage: lein distribute <environment-name>"))
  (System/exit 0))
