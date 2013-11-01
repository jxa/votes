(ns voter.core
  (:require [clojure.browser.repl :as repl]
            [dommy.core :as dommy]
            [voter.hangout :as hangout])
  (:use-macros
    [dommy.macros :only [node sel sel1 deftemplate]]))


(defn log [msg]
  (.log js/console msg))


(deftemplate person-t [person votes {:keys [all-votes-cast me]}]
  (let [person-id (.-id person)
        vote (get votes person-id)
        vote-class (cond
                    (= me person-id) "visible"
                    all-votes-cast   "visible"
                    :else            "")]
    [:li.person {:class (if (empty? vote) "no-vote" "voted")}
     [:div.name (.-displayName person)]
     [:div.picture
      [:img.avatar {:src (.-url (.-image person))}]
      [:div.vote {:class vote-class} vote]]]))

;; TODOs ...
;; figure out the timing issue on app load (or is it a load issue?)
;; gapi.hangout.layout.displayNotice to show when a vote is opened
;; John has opened a new voting issue...
;; Abstain from voting (but you want to see the vote results)
;;   - maybe implemented with a simple "Abstain" checkbox + flag

(defn cast-my-vote [e]
  (.preventDefault e)
  (let [input (sel1 :#my-vote)
        vote (dommy/value input)]
    (hangout/set-shared-state (hangout/my-id) vote)
    (dommy/set-value! input "")))

(defn clear-all-votes [e]
  (.preventDefault e)
  (hangout/clear-shared-state))

(defn init-ui []
  (dommy/append! (sel1 :#voter_content)
                 [:ul#voters]
                 [:div#voting-booth
                  [:form#vote-form
                   [:input#my-vote {:name "my-vote"}]
                   [:input {:type "submit" :value "Vote!"}]]
                  [:button#clear-all "Reset Votes"]])
  (dommy/listen! (sel1 :#vote-form)
                 :submit cast-my-vote)
  (dommy/listen! (sel1 :#clear-all)
                 :click clear-all-votes))

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

(defn init []
  (log "hangout loaded")
  (init-ui)
  (render-participants (hangout/enabled-participants) (hangout/shared-state))
  (hangout/on-participants-change (fn [e]
                                    (log "We have new participants")
                                    (render-participants (hangout/enabled-participants)
                                                         (hangout/shared-state))))
  (hangout/on-state-change (fn [e]
                             (log "state changed")
                             (log (.-state e))
                             (render-participants (hangout/enabled-participants)
                                                  (.-state e)))))

(hangout/on-hangout-ready init)


;; (repl/connect "http://localhost:9000/repl")
