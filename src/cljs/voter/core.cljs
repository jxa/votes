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

(defn bind-dom []
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
            ]]]))



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

(defn update-votes [state participants]
  (map (fn [person]
         (assoc person :vote (aget state (:id person))))
       participants))

(defn make-local-state [participants shared-state my-id]
  (let [people (->> participants
                    (map participant->map)
                    (update-votes shared-state))
        vote-count (count (filter (complement empty?) (map :vote people)))
        people-count (count people)
        vote-state (aget shared-state vote-state-key)]
    {:participants people
     :my-id my-id
     :vote-state (cond
                  (= 0 vote-count)            :open
                  (= vote-count people-count) :closed
                  (not (empty? vote-state))   (keyword vote-state)
                  :else                       :open)
     :transition-type (cond
                       (= 0 vote-count)            :none
                       (= vote-count people-count) :reset
                       (= vote-state "closed")     :reset
                       (< vote-count people-count) :close
                       :else                       :none)}))

(defn update-local-state [ & [participants state id]]
  (reset! local-state
          (make-local-state (or participants (hangout/enabled-participants))
                            (or state (hangout/shared-state))
                            (or id (hangout/my-id))))
  (pp @local-state))

(defn init []
  (bind-dom)
  (update-local-state)

  ;; Bind form events
  (event/on-raw "#vote-form" :submit cast-my-vote)
  (event/on-raw "#change-vote-state" :click change-vote-state)

  ;; google hangout event handling
  (hangout/on-participants-change
   (fn [e] (update-local-state (.-enabledParticipants e))))

  (hangout/on-state-change
   (fn [e] (update-local-state nil (.-state e))))

  (hangout/on-message-received
   (fn [e]
     (hangout/notice (.-message e)))))

(defn ^:export run []
  (event/on-load
   (fn []
     (hangout/on-hangout-ready init))))


;; BREPL development

(when (aget js/window "__include_brepl")
  (bind-dom)
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
