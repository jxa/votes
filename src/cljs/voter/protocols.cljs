(ns voter.protocols)

(defprotocol IHangoutApi
  (initialize [this state-listener] "Register HangoutState listener")
  (enabled-participants [this] "Hangout participants who have enabled EstimationParty")
  (display-notice [this message] "Display a message for the current user")
  (broadcast-notice [this message] "Send a message to all participants")
  (shared-state [this] "Retrieve shared data object")
  (reset-shared-state [this] "Sets values equal to empty string for all current keys")
  (set-shared-state [this key val] "Insert a KV pair into shared hangout state"))

(defprotocol IHangoutState
  (initialized [this hangout local-participant])
  (state-changed [this hangout shared-state]
    "Called when shared-state is updated. Also called after initialized.")
  (participants-changed [this hangout participants]
    "Called when participants join or leave. Also called after initialized.")
  (message-received [this hangout message] "Called when a hangouts message is received"))
