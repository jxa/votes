(ns voter.dev-hangout
  (:require [voter.protocols :as proto]))


(defprotocol IDevHangout
  (add-participant [this participant])
  (set-current-participant [this participant])
  (init-listener [this listener]))

(defrecord DevHangout [state participants current-participant listener]
  IDevHangout
  (add-participant [this participant]
    (swap! participants conj participant))

  (set-current-participant [this participant]
    (reset! current-participant participant))

  (init-listener [this listener]
    (add-watch participants ::participants-watch
               (fn [key ref old new]
                 (proto/participants-changed listener this (clj->js new))))
    (add-watch state ::state-watch
               (fn [key ref old new]
                 (proto/state-changed listener this (clj->js new)))))


  proto/IHangoutApi
  (initialize [this state-listener]
    (let [this (assoc this
                 :listener state-listener
                 :participants (or participants (atom []))
                 :state (or state (atom {}))
                 :current-participant (or current-participant (atom nil)))]
      (init-listener this state-listener)
      (proto/initialized state-listener this @(:current-participant this))
      (proto/participants-changed state-listener this @(:participants this))
      this))

  (enabled-participants [this]
    @participants)

  (display-notice [this message]
    (. js/console log message))

  (broadcast-notice [this message]
    (. js/console log message))

  (shared-state [this]
    @state)

  (reset-shared-state [this]
    (reset! state {}))

  (set-shared-state [this key val]
    (swap! state assoc key val)))
