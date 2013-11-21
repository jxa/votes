(ns voter.hangout)

;; this blows up in development mode (without hangout api)
;; TODO: include the hangouts api in the build process?
(when (.hasOwnProperty js/window "gapi")
  (def hangout (.-hangout js/gapi))
  (def hangout-data (.-data hangout))
  (def hangout-layout (.-layout hangout)))

(defn on-hangout-ready
  "add callback which fires after initialization is complete"
  [fun]
  (.add (.-onApiReady hangout)
        (fn [e]
          (fun))))

(defn on-participants-change
  "call fun whenever the set of participants running the app changes"
  [fun]
  (.add (.-onEnabledParticipantsChanged hangout) fun))

(defn on-state-change
  "Call function callback when shared-state is updated"
  [fun]
  (.add (.-onStateChanged hangout-data) fun))

(defn on-message-received
  "Call function when message received via broadcast-notice"
  [fun]
  (.add (.-onMessageReceived hangout-data) fun))

(defn my-id []
  (.-id (.-person (.getLocalParticipant hangout))))

(defn my-name []
  (.-displayName (.-person (.getLocalParticipant hangout))))

(defn enabled-participants
  "Return all participants currently engaged in the Estimation Party"
  []
  (.getEnabledParticipants hangout))

(defn shared-state []
  (.getState hangout-data))

(defn shared-state-keys []
  (.getKeys hangout-data))

(defn set-shared-state
  "Set a single value into shared state. Prefer update-shared-state
   for updating multiple values since updates are rate-limited"
  [key value]
  (.setValue hangout-data key value))

(defn update-shared-state
  "updates shared state with given map of properties"
  [m]
  (.submitDelta hangout-data (clj->js m)))

(defn clear-shared-state []
  "clear the values of all current keys"
  (update-shared-state
   (zipmap (shared-state-keys) (repeat ""))))

(defn notice
  "Display a message pop-up to the current user"
  [msg]
  (.displayNotice hangout-layout msg))

(defn broadcast-notice
  "Broadcast a message to all hangout participants,
   including current user."
  [msg]
  (.sendMessage hangout-data msg)
  (notice msg))
