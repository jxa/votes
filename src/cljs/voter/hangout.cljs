(ns voter.hangout
  (:require [voter.protocols :as proto]))

(defprotocol IGoogleHangout
  (layout [this]
    "Convenient access to gapi.hangout.layout")
  (data [this]
    "Convenient access to gapi.hangout.data")
  (on-hangout-ready [this f]
    "add callback which fires after initialization is complete")
  (on-participants-change [this f]
    "call fun whenever the set of participants running the app changes")
  (on-state-change [this f]
    "Call function callback when shared-state is updated")
  (on-message-received [this f]
    "Call function when message received via broadcast-notice")
  (init-listener [this listener]
    "set up callbacks which will update listener.
     listener must extend HangoutState protocol"))


(defn shared-state-keys [hangout]
  (.getKeys (data hangout)))

(defn update-shared-state
  "updates shared state with given map of properties"
  [hangout m]
  (.submitDelta (data hangout) (clj->js m)))

(defn clear-shared-state
  "clear the values of all current keys"
  [hangout]
  (update-shared-state hangout
   (zipmap (shared-state-keys hangout) (repeat ""))))

(defn participant->map
  "Return a map of participant attributes that we care about"
  [participant]
  (let [person (.-person participant)]
    {:id (.-id person)
     :name (.-displayName person)
     :img-url (.-url (.-image person))}))


(defrecord GoogleHangout [hangout listener]
  IGoogleHangout
  (layout [this]
    (.-layout hangout))

  (data [this]
    (.-data hangout))

  (on-hangout-ready [this fun]
    (.add (.-onApiReady hangout)
          (fn [e] (fun))))

  (on-participants-change [this fun]
    (.add (.-onEnabledParticipantsChanged hangout) fun))

  (on-state-change [this fun]
    (.add (.-onStateChanged (data this)) fun))

  (on-message-received [this fun]
    (.add (.-onMessageReceived (data this)) fun))

  (init-listener [this listener]
    (on-participants-change this
     (fn [e]
       (proto/participants-changed
        listener this (map participant->map (.-enabledParticipants e)))))
    (on-state-change this
     (fn [e]
       (proto/state-changed listener this (.-state e))))
    (on-message-received this
     (fn [e]
       (proto/message-received listener this (.-message e)))))


  proto/IHangoutApi
  (initialize [this state-listener]
    (let [this (assoc this :listener state-listener)]
      (on-hangout-ready this
       #(do
          (init-listener this state-listener)
          (proto/initialized
           listener this (participant->map (.getLocalParticipant hangout)))))
      this))

  (enabled-participants [this]
    (map participant->map (.getEnabledParticipants hangout)))

  (display-notice [this message]
    (.displayNotice (layout this) message))

  (broadcast-notice [this message]
    (.sendMessage (data this) message)
    (proto/display-notice this message))

  (shared-state [this]
    (.getState (data this)))

  (reset-shared-state [this]
    (clear-shared-state hangout))

  (set-shared-state [this key val]
    (.setValue (data this) key val)))
