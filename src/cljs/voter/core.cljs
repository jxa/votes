(ns voter.core
  (:require [clojure.browser.repl :as repl]
            [dommy.core :as dommy])
  (:use-macros
    [dommy.macros :only [node sel sel1 deftemplate]]))


;; class Participant(
;;   id,
;;   displayIndex,
;;   hasMicrophone,
;;   hasCamera,
;;   hasAppEnabled,
;;   isBroadcaster,
;;   isInBroadcast,
;;   locale,
;;   person,
;;   person.id,
;;   person.displayName,
;;   person.image,
;;   person.image.url
;; )

(deftemplate person-t [p]
  [:div.person
   [:div.name (.-displayName p)]
   [:img.avatar {:src (.-url (.-image p))}]])

(defn log [msg]
  (.log js/console msg))

(def hangout (.-hangout js/gapi))
(def hangout-data (.-data hangout))

(defn on-hangout-ready
  "add callback which fires after initialization is complete"
  [fun]
  (.add (.-onApiReady hangout) fun))

(defn enabled-participants []
  (.getEnabledParticipants hangout))

(defn on-participants-change
  "call fun whenever the set of participants running the app changes"
  [fun]
  (.add (.-onEnabledParticipantsChanged hangout) fun))

(defn on-state-change [fun]
  (.add (.-onStateChanged hangout-data) fun))

(defn set-shared-state [key value]
  (.setValue hangout-data key value))

(defn clear-shared-state []
  (let [keys (.getKeys hangout-data)]
    (.submitDelta hangout-data
                  (clj->js
                   (into {} (map
                             (fn [key] [key ""])
                             keys))))))

(defn my-id []
  (.getLocalParticipantId hangout))

(defn cast-my-vote [e]
  (.preventDefault e)
  (let [input (sel1 :#my-vote)
        vote (dommy/value input)]
    (log (str "my vote is " vote))
    (set-shared-state (my-id) vote)
    (dommy/set-value! input "")))

(defn clear-all-votes [e]
  (log "clearing all votes")
  (clear-shared-state))

(defn init-ui []
  (dommy/append! (sel1 :#voter_content)
                 [:div#voters]
                 [:div#voting-booth
                  [:form#vote-form
                   [:input#my-vote {:name "my-vote"}]
                   [:input {:type "submit"}]]
                  [:button#clear-all "Clear All Votes"]])
  (dommy/listen! (sel1 :#vote-form)
                 :submit cast-my-vote)
  (dommy/listen! (sel1 :#clear-all)
                 :click clear-all-votes))

(defn render-participants [participants]
  (dommy/replace-contents!
   (sel1 :#voters)
   (map person-t
        (map #(.-person %) participants))))

(defn init []
  (log "hangout loaded")
  (init-ui)
  (render-participants (enabled-participants))
  (on-participants-change (fn [e]
                            (log "We have new participants")
                            (render-participants (enabled-participants))))
  (on-state-change (fn [e]
                     (log "state changed")
                     (log (.-state e)))))

(on-hangout-ready (fn [e]
                    (if (.-isApiReady e)
                      (init))))

(log "code loaded")


;; (repl/connect "http://localhost:9000/repl")
