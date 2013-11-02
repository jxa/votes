(ns voter.core
  (:require
   [clojure.browser.repl :as repl]
   [voter.hangout :as hangout]
   [c2.core :refer [unify]]
   [c2.dom :as dom]
   [c2.event :as event])
  (:use-macros
   [c2.util :only [bind! pp]]))


(defn log [msg]
  (.log js/console msg))


(def participants (atom []))
(def my-id (atom ""))


(defn person-t [{:keys [id name img-url vote]}]
  [:li.person {:class (when-not (empty? vote) "voted")}
   [:div.name name]
   [:div.picture
    [:img.avatar {:src img-url}]
    [:div.vote {:class (when (= @my-id id) "visible")}
     vote]]])

(bind! "#voter_content"
       [:div#content-inner
        [:ul#voters
         {:class (when-not (some empty? (map :vote @participants))
                   "all-votes-cast")}
         (unify @participants person-t)]
        [:div#voting-booth
         [:form#vote-form
          [:input#my-vote {:name "my-vote"}]
          [:input {:type "submit" :value "Vote!"}]]
         [:button#clear-all "Reset Votes"]]])

;; TODOs ...
;; gapi.hangout.layout.displayNotice to show when a vote is opened
;; John has opened a new voting issue...
;; Abstain from voting (but you want to see the vote results)
;;   - maybe implemented with a simple "Abstain" checkbox + flag

(defn cast-my-vote [e]
  (.preventDefault e)
  (let [input (dom/select "#my-vote")
        vote (dom/val input)]
    (hangout/set-shared-state (hangout/my-id) vote)
    (dom/val input "")))

(defn clear-all-votes [e]
  (.preventDefault e)
  (hangout/clear-shared-state))


(defn participant-votes [people state]
  (into {} (map (fn [p]
                  [(.-id p) (aget state (.-id p))])
                people)))

(defn render-participants [participants state]
  (let [people (map #(.-person %) participants)
        me (hangout/my-id)
        votes (participant-votes people state)
        all-votes-cast (every? (complement empty?) (vals votes))]
    (dommy/replace-contents!
     (sel1 :#voters)
     (map (fn [person]
            (person-t person votes {:all-votes-cast all-votes-cast :me me}))
          people))))

(comment
  participant
  {:id ""
   :vote ""
   :name ""
   :img-url ""
   })


(defn init []
  (log "hangout loaded")

  (reset! my-id (hangout/my-id))

  (event/on-raw "#vote-form" :submit cast-my-vote)
  (event/on-raw "#clear-all" :click clear-all-votes)

  ;; TODO: This needs to change so it's only adding / removing from the participants atom
  (render-participants (hangout/enabled-participants) (hangout/shared-state))
  (hangout/on-participants-change (fn [e]
                                    (log "We have new participants")
                                    (render-participants (hangout/enabled-participants)
                                                         (hangout/shared-state))))
  ;; TODO: only update the participants who changed
  (hangout/on-state-change (fn [e]
                             (log "state changed")
                             (log (.-state e))
                             (render-participants (hangout/enabled-participants)
                                                  (.-state e)))))

;; (event/on-load
;;       (fn []
;;         (hangout/on-hangout-ready init)))

(when (aget js/window "__include_brepl")
  (repl/connect "http://localhost:9000/repl"))
