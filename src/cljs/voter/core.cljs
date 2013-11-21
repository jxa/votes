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
(def vote-state (atom :open))
(def vote-state-key "-estimation-party-state")

(defn person-t [{:keys [id name img-url vote] :as person}]
  [:li.person {:class (str (when-not (empty? vote) "voted")
                           (when (= @my-id id) " visible"))}
   [:div.picture
    [:img.avatar {:src img-url}]
    [:div.backface
     [:img.back-avatar {:src img-url}]
     [:div.vote {:class (when (= @my-id id) "visible")}
      vote]]]
   [:div.name name]])

(bind! "#voter-content"
       (let [closed? (= @vote-state :closed)
             display-votes (when (or closed?
                                     (not (some empty? (map :vote @participants))))
                             "display-votes")]
         [:div#content-inner
          [:ul#voters
           {:class display-votes}
           (unify @participants person-t)]
          [:div#voting-booth
           [:form#vote-form
            [:input#my-vote {:name "my-vote" :autofocus true :autocomplete "off"
                             :maxlength 4 :size 4 :placeholder "Enter Vote"}]
            [:input#submit-vote {:type "submit" :value "Vote"}]]
           [:button#change-vote-state
            {:class display-votes}
            (if display-votes
              "Reset All Votes"
              "End Current Vote")]]]))



;; Form Event Handlers

(defn cast-my-vote [e]
  (.preventDefault e)
  (if (= :open @vote-state)
    (let [input (dom/select "#my-vote")
          vote (dom/val input)]
      (hangout/set-shared-state (hangout/my-id) vote)
      (dom/val input ""))
    (hangout/notice "Voting is currently closed.")))


(defn change-vote-state [e]
  (.preventDefault e)
  (if (= :open @vote-state)
    (do
      ;; TODO: broadcast message
      (hangout/set-shared-state vote-state-key "closed"))
    (do
      (hangout/set-shared-state vote-state-key "open")
      (hangout/clear-shared-state))))


;; Updating local state

(defn participant->map [participant]
  (let [person (.-person participant)]
    {:id (.-id person)
     :name (.-displayName person)
     :img-url (.-url (.-image person))}))

(defn included-in? [c thing]
  (some (set thing) c))

(defn update-participant
  "People is a vector of maps. Find the map identified by id and update the value at key"
  [people id key val]
  (map (fn [person]
         (if (= id (:id person))
           (assoc person key val)
           person))
       people))

(defn update-participants [current new]
  (let [cur-ids (set (map :id current))
        new-ids (set (map #(.-id (.-person %)) new))]
    (into
     (filter #(included-in? new-ids (:id %)) current)
     (map participant->map
          (remove #(included-in? cur-ids (.-id (.-person %)))
                  new)))))

(defn update-votes [participants state]
  (map (fn [person]
         (assoc person :vote (aget state (:id person))))
       participants))

(defn update-vote-state [hangout-state]
  (let [state (aget hangout-state vote-state-key)]
    (when (and (not (empty? state))
               (not= (name @vote-state) state))
      (reset! vote-state (keyword state)))))


(defn init []

  ;; Setup current state
  (reset! my-id (hangout/my-id))
  (let [people (map participant->map (hangout/enabled-participants))]
    (reset! participants (update-votes people (hangout/shared-state))))

  ;; Bind form events
  (event/on-raw "#vote-form" :submit cast-my-vote)
  (event/on-raw "#change-vote-state" :click change-vote-state)

  ;; google hangout event handling
  (hangout/on-participants-change
   (fn [e]
     (swap! participants update-participants (.-enabledParticipants e))))

  (hangout/on-state-change
   (fn [e]
     (update-vote-state (.-state e))
     (swap! participants update-votes (.-state e)))))

(defn ^:export run []
  (event/on-load
   (fn []
     (hangout/on-hangout-ready init))))


;; BREPL development

(when (aget js/window "__include_brepl")
  (repl/connect "http://localhost:9000/repl")
  (defn data! []
    (reset! my-id "2")
    (reset! participants
            [{:id "1", :name "John Andrews", :img-url "http://lorempixel.com/50/50", :vote ""}
             {:id "2", :name "Chandu Tennety", :img-url "http://lorempixel.com/50/50", :vote "2"}]))
  (data!))


;; TODOs ...
;; gapi.hangout.layout.displayNotice to show when a vote is opened
;;    -- or reset "Derek has reset the vote"
;; Abstain from voting (but you want to see the vote results)
;;   - maybe implemented with a simple "Abstain" checkbox + flag
;; disable clicking on "reset all votes" until all votes are cast

;; Feedback from first ICANN party
;; more visible votes
;; scrollable votes area
;; "End Voting Early" button
;; prevent voting after reveal
;; help user understand that they can change their vote
;; intro video
