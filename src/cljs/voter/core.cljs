(ns voter.core
  (:require [dommy.core :as dommy]
            [voter.hangout :as hangout])
  (:use-macros
    [dommy.macros :only [node sel sel1 deftemplate]]))


(defn log [msg]
  (.log js/console msg))


(deftemplate person-t [person votes {:keys [all-votes-cast me]}]
  (let [person-id (.-id person)
        vote-class (cond
                    (= me person-id) "me"
                    all-votes-cast   "visible"
                    :else            "hidden")]
    [:div.person
     [:div.name (.-displayName person)]
     [:img.avatar {:src (.-url (.-image person))}]
     [:div.vote
      {:class vote-class}
      (get votes person-id)]]))


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

(defn participant-votes [people state]
  (into {} (map (fn [p]
                  [(.-id p) (aget state (.-id p))])
                people)))

(defn render-participants [participants state]
  (let [people (map #(.-person %) participants)
        me (hangout/my-id)
        votes (participant-votes people state)
        all-votes-cast (every? (complement empty?) votes)]
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
