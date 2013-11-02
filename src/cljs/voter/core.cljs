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


(defn person-t [{:keys [id name img-url vote] :as person}]
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


;; Form Event Handlers

(defn cast-my-vote [e]
  (.preventDefault e)
  (let [input (dom/select "#my-vote")
        vote (dom/val input)]
    (hangout/set-shared-state (hangout/my-id) vote)
    (dom/val input "")))

(defn clear-all-votes [e]
  (.preventDefault e)
  (hangout/clear-shared-state))


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



(defn init []
  (log "hangout loaded")

  ;; Setup current state
  (reset! my-id (hangout/my-id))
  (let [people (map participant->map (hangout/enabled-participants))]
    (reset! participants (update-votes people (hangout/shared-state))))

  ;; Bind form events
  (event/on-raw "#vote-form" :submit cast-my-vote)
  (event/on-raw "#clear-all" :click clear-all-votes)

  ;; google hangout event handling
  (hangout/on-participants-change
   (fn [e]
     (log "We have new participants")
     (swap! participants update-participants (.-enabledParticipants e))))

  (hangout/on-state-change
   (fn [e]
     (log "state changed")
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
            [{:id "1", :name "John", :img-url "http://i206.photobucket.com/albums/bb22/cherrycreamsoda_photos/david-hasselhoff-07.jpg", :vote ""}
             {:id "2", :name "Chandu", :img-url "http://i206.photobucket.com/albums/bb22/cherrycreamsoda_photos/david-hasselhoff-07.jpg", :vote "2"}])))

;; TODOs ...
;; gapi.hangout.layout.displayNotice to show when a vote is opened
;; John has opened a new voting issue...
;; Abstain from voting (but you want to see the vote results)
;;   - maybe implemented with a simple "Abstain" checkbox + flag
