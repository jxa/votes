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

(def local-state
  (atom {:participants []
         :my-id ""
         :vote-state :open
         :transition-type :none}))

(def vote-state-key "-estimation-party-state")

(defn person-t [{:keys [id name img-url vote] :as person}]
  (let [vote-visibility (when (= (:my-id @local-state) id) "visible")]
    [:li.person {:class (str vote-visibility (when-not (empty? vote) " voted"))}
     [:div.picture
      [:img.avatar {:src img-url}]
      [:div.backface
       [:img.back-avatar {:src img-url}]
       [:div.vote {:class vote-visibility} vote]]]
     [:div.name name]]))

(bind! "#voter-content"
       [:div#content-inner
        [:ul#voters
         {:class (when (= :closed (:vote-state @local-state)) "display-votes")}
         (unify (:participants @local-state) person-t)]
        [:div#voting-booth
         [:form#vote-form
          [:input#my-vote {:name "my-vote" :autofocus true :autocomplete "off"
                           :maxlength 4 :size 4 :placeholder "Enter Vote"}]
          [:input#submit-vote {:type "submit" :value "Vote"}]]
         [:button#change-vote-state
          {:class (name (:transition-type @local-state))}
          (if (= :reset (:transition-type @local-state))
            "Reset All Votes"
            "End Current Vote")
          ]]])



;; Form Event Handlers

(defn cast-my-vote [e]
  (.preventDefault e)
  (if (= :open (:vote-state @local-state))
    (let [input (dom/select "#my-vote")
          vote (dom/val input)]
      (hangout/set-shared-state (hangout/my-id) vote)
      (dom/val input ""))
    (hangout/notice "Voting is currently closed.")))


(defn change-vote-state [e]
  (.preventDefault e)
  (case (:transition-type @local-state)
    :reset (do
             (hangout/broadcast-notice (str (hangout/my-name) " has opened a new vote"))
             (hangout/clear-shared-state)
             (hangout/set-shared-state vote-state-key "open"))
    :close (do
             (hangout/broadcast-notice (str (hangout/my-name) " has closed the vote"))
             (hangout/set-shared-state vote-state-key "closed"))
    (log (str "Can't transition from state" (:transition-type @local-state)))))


;; Updating local state

(defn participant->map [participant]
  (let [person (.-person participant)]
    {:id (.-id person)
     :name (.-displayName person)
     :img-url (.-url (.-image person))}))

(defn update-participant
  "People is a vector of maps. Find the map identified by id and update the value at key"
  [people id key val]
  (map (fn [person]
         (if (= id (:id person))
           (assoc person key val)
           person))
       people))

(defn update-votes [participants state]
  (map (fn [person]
         (assoc person :vote (aget state (:id person))))
       participants))

(defn make-local-state [participants shared-state my-id]
  (let [people (-> participants
                   participant->map
                   (update-votes shared-state))
        votes (filter (complement empty?) (map :vote people))
        vote-state (aget shared-state vote-state-key)]
    {:participants people
     :my-id my-id
     :vote-state (cond
                  (= 0 (count votes))              :open
                  (= (count votes) (count people)) :closed
                  :else                            vote-state)
     :transition-type (cond
                       (= (count votes) (count people)) :reset
                       (< (count votes) (count people)) :close
                       :else                            :none)}))

(defn update-local-state [ & args]
  (reset! local-state (make-local-state (hangout/enabled-participants)
                                  (hangout/shared-state)
                                  (hangout/my-id))))

(defn init []

  (update-local-state)

  ;; Bind form events
  (event/on-raw "#vote-form" :submit cast-my-vote)
  (event/on-raw "#change-vote-state" :click change-vote-state)

  ;; google hangout event handling
  (hangout/on-participants-change update-local-state)
  (hangout/on-state-change update-local-state)
  (hangout/on-message-received
   (fn [e]
     (hangout/notice (.-message e)))))

(defn ^:export run []
  (event/on-load
   (fn []
     (hangout/on-hangout-ready init))))


;; BREPL development

(when (aget js/window "__include_brepl")
  (repl/connect "http://localhost:9000/repl")
  (defn data! []
    (swap! local-state assoc :my-id "2")
    (swap! local-state assoc :participants
           [{:id "1", :name "John Andrews", :img-url "http://lorempixel.com/50/50", :vote ""}
            {:id "2", :name "Chandu Tennety", :img-url "http://lorempixel.com/50/50", :vote "2"}]))
  (data!))


;; == Feedback from first ICANN party ==
;; "End Voting Early" button
;; prevent voting after reveal
;; help user understand that they can change their vote
;; intro video
