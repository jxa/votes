(ns voter.core
  (:require
   [clojure.browser.repl :as repl]
   [voter.protocols :as proto]
   [voter.hangout :as hangout]
   [voter.dev-hangout :as dev]
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
         :my-name ""
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

(defn cast-my-vote [hangout e]
  (.preventDefault e)
  (if (= :open (:vote-state @local-state))
    (let [input (dom/select "#my-vote")
          vote (dom/val input)]
      (proto/set-shared-state hangout (:my-id @local-state) vote)
      (dom/val input ""))
    (proto/display-notice hangout "Voting is currently closed.")))


(defn change-vote-state [hangout e]
  (.preventDefault e)
  (case (:transition-type @local-state)
    :reset (do
             (proto/broadcast-notice hangout (str (:my-name @local-state) " has opened a new vote"))
             (proto/reset-shared-state hangout)
             (proto/set-shared-state hangout vote-state-key "open"))
    :close (do
             (proto/broadcast-notice hangout (str (:my-name @local-state) " has closed the vote"))
             (proto/set-shared-state hangout vote-state-key "closed"))
    (log (str "Can't transition from state" (:transition-type @local-state)))))


;; Updating local state

(defn update-votes [state participants]
  (map (fn [person]
         (assoc person :vote (aget state (:id person))))
       participants))

(defn make-local-state [participants shared-state]
  (let [people (update-votes shared-state participants)
        vote-count (count (filter (complement empty?) (map :vote people)))
        people-count (count people)
        vote-state (aget shared-state vote-state-key)]
    {:participants people
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

(defn update-local-state [hangout & [participants state]]
  (swap! local-state merge
         (make-local-state (or participants (proto/enabled-participants hangout))
                           (or state (proto/shared-state hangout)))))


(def hangout-api (atom nil))

(defn init [api]
  (bind-dom)
  (reset! hangout-api
          (proto/initialize
           api
           (reify proto/IHangoutState
             (initialized [_ api local-participant]
               (event/on-raw "#vote-form" :submit (partial cast-my-vote api))
               (event/on-raw "#change-vote-state" :click (partial change-vote-state api))
               (swap! local-state assoc
                      :my-id (:id local-participant)
                      :my-name (:name local-participant)))
             (state-changed [_ api shared-state]
               (update-local-state api nil shared-state))
             (participants-changed [_ api participants]
               (update-local-state api participants))
             (message-received [_ api message]
               (proto/display-notice api message))))))

(defn ^:export run [gapi-hangout]
  (event/on-load
   #(init (hangout/map->GoogleHangout {:hangout gapi-hangout}))))

(defn ^:export run-dev []
  (event/on-load
   #(do
      (repl/connect "http://localhost:9000/repl")
      (init (dev/map->DevHangout
             {:participants (atom [{:id "1", :name "John Andrews",
                                    :img-url "http://lorempixel.com/50/50"}
                                   {:id "2", :name "Chandu Tennety",
                                    :img-url "http://lorempixel.com/75/75"}])
              :current-participant (atom {:id "1", :name "John Andrews",
                                          :img-url "http://lorempixel.com/50/50"})})))))
