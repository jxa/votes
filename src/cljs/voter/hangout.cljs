(ns voter.hangout)

;; this blows up in development mode (without hangout api)
;; TODO: include the hangouts api in the build process?
(when (.hasOwnProperty js/window "gapi")
  (def hangout (.-hangout js/gapi))
  (def hangout-data (.-data hangout)))

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

(defn on-state-change [fun]
  (.add (.-onStateChanged hangout-data) fun))

(defn my-id []
  (.-id (.-person (.getLocalParticipant hangout))))

(defn enabled-participants []
  (.getEnabledParticipants hangout))

(defn shared-state []
  (.getState hangout-data))

(defn shared-state-keys []
  (.getKeys hangout-data))

(defn set-shared-state [key value]
  (.setValue hangout-data key value))

(defn update-shared-state
  "updates shared state with given map of properties"
  [m]
  (.submitDelta hangout-data (clj->js m)))

(defn clear-shared-state []
  (update-shared-state
   (zipmap (shared-state-keys) (repeat ""))))
