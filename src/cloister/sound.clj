(ns cloister.sound
  (:require [clojure.set])
  (:require [cloister.bindings.audio :as a])
  (:import (org.lwjgl.util WaveData))
  (:import (org.lwjgl BufferUtils)))

; The idea behind the sound engine is to have three alternatives.
;
; For simple fire-and-forget sound effects it's just possible to call
; something like (play-fx :soundname position some-other-data) and
; forget all about it, it's just a sound effect we don't care about anymore.
; It's not looping and we don't keep a handle on it.
;
; For long-lasting and easy-to-use non-positional music (like bgm) there
; is going to be a global map of currently-playing music (looped) and
; any entity can play/stop any (or all) currently-playing songs.
; Using a simple interface like (play-bgm :bgm-name) or (stop-bgm :bgm-name)
;
; For developers who require a more advanced sound management system,
; there's going to be a lower-level (but still high enough) wrapper
; around actual OpenAL capabilities where entities can become sources
; and store audio data inside their state to tweak and play and modify
; as they see fit. This potentially allows the implementation of continuous
; sounds, more advanced sound effects and play with the various tweaking from
; OpenAL.
; The interface would be something like (create-sound :name) which returns
; a tweakeble source, from there it's possible to use (play-sound s) or
; (pause-sound s) (stop-sound s), where s is the data returned from
; the create-sound call. To change parameters a simple (set-sound-data s data)
; where data is an associative map of key -> value for various OpenAL properties.
; (More about that later)
;
; NOTE: Have to investigate how global volumes and stuff like that works
;       with OpenAL

; Dictionary of soundname -> id where id is the buffer id in the OpenAL
; device.
(def CLOISTER_SOUNDMAP (atom {}))

(defn assert-no-error
  "Check if there was any audio-related error and throws an exception in
  such case."
  [message]
  (let [e (a/get-error)]
    (when-not (= e a/no-error) (throw (Exception. (str "AudioError " e ": " message))))))

(defn create-float-buffer
  "Create a new float buffer with the given args"
  [values]
  (let [b (BufferUtils/createFloatBuffer (count values))]
    (doall (map #(.put b (float %)) values))
    (.rewind b)))

(def default-listener { :position (create-float-buffer [0 0 0])
                        :velocity (create-float-buffer [0 0 0])
                        :orientation (create-float-buffer [0 0 -1 0 1 0])
                       })

(def default-source { :pitch 1.0
                      :gain 1.0
                      :position (create-float-buffer [0 0 0])
                      :velocity (create-float-buffer [0 0 0])
                     })

(defn load-buffer
  "Load a wave file inside a buffer"
  [buf-id wavefile]
  (a/buffer-data buf-id (.format wavefile) (.data wavefile) (.samplerate wavefile)))

(defn load-sound!
  "Load a new sound into memory. Has side effects."
  [id file]
  (when (nil? (id @CLOISTER_SOUNDMAP))
    (let [fin (java.io.FileInputStream. file)
          bis (java.io.BufferedInputStream. fin)
          wd (WaveData/create bis)
          b (BufferUtils/createIntBuffer 1)]
      (.close bis)
      (.close fin)
      (a/generate-buffers b)
      (when (nil? wd)
        (throw (Exception. "Unable to load WaveData " file)))
      (assert-no-error (format "Unable to load the audio resource %s" id))
      (load-buffer (.get b 0) wd)
      (.dispose wd)
      (swap! CLOISTER_SOUNDMAP assoc id (.get b 0))))
  nil)

(defn unload-sound!
  "Remove a buffer from memory. Has side effects.
  WARNING: Make sure the buffer is not being used anymore before unloading it."
  [id]
  (let [b (id @CLOISTER_SOUNDMAP)]
    (a/delete-buffers b)
    (assert-no-error (format "Unable to delete sound %s, maybe it's still in use?" id))))

; There needs to be a source manager that takes care of swapping in/out
; sources with a maximum of 32 concurrent sources.
; MAX = 32 sources
; keep a list of all the sources and make sure to flag those that are active
; among all active sources, provide a way to test for priority (distance from listener?)
; When a new play request is received, hunt for a free source (or kill an already playing one)
; and pass it back to the entity. The entity receives an ID that acts as a ticket in the source
; queue, the entity can always query the play queue to see if the ticket is still active (or if
; it's been recycled and it needs to request a new ticket). The entity can keep hold of that
; ticket as much as it wants but if the ticket isn't active anymore (because it got recycled)
; then all the sound events sent with that ticket will not play at all.
; BGM and soundfx have the highest priorities because BGM should always be playing at the
; top-most and soundfx should be short and uncontrolled and get recycled right after the sound
; is done playing.
; Tickets are implemented by an ever-growing unique ID/int value.

(def max-sources 16)

; Map of currently existing tickets, must always be <= max-sources in size
(def ticket-map (ref {}))

; Ticket generator, always giving unique IDs
(def ticket-gen (atom 0))

(def ticket-base
  {
   :id 0
   :source 0
   :type :sample ; can either be :bgm :soundfx or :sample. Priority :bgm > :soundfx > :sample
   :playing? false
   })

; Actual sources in the game, it's a set because sources are unique
(def source-list (ref #{}))

(defn set-source-data
  "High level function to tweak parameters of a source."
  [source data]
  (doall (map (fn [[pname value]]
                (if (= pname :buffer)
                  (a/set-source-value source (pname a/properties) (value @CLOISTER_SOUNDMAP))
                  (a/set-source-value source (pname a/properties) value))
                (assert-no-error (format "Unable to set %s propery for source %s." pname source))) data)))

(defn spawn-ticket
  "Spawn a new ticket progressively."
  [source data type]
  (let [id (swap! ticket-gen inc)]
    (set-source-data source (merge default-source data))
    (merge (assoc ticket-base :id id :source source :type type) data)))

(defn get-lowest-priority
  "Return the ticket id with the lowest priority in the ticket list"
  []
  ; TODO - this now only removes the oldest ticket
  ; TODO - set up a customizable scheduling algorithm for sources
  (reduce #(min %1 (first %2)) @ticket-gen @ticket-map))

(defn source-playing?
  "If a source is playing at the moment."
  [source]
  (= (a/get-source-value source (:source-state a/properties) :int)
     (:playing a/state-map)))

(defn update-tickets
  "Update the state of all the tickets in the ticket map"
  []
  (dosync
   (alter ticket-map (fn [m]
                       (into {} (do (map (fn [[id val]]
                                           (if (and (= (:type val) :soundfx)
                                                    (not (source-playing? (:source val))))
                                             nil
                                             [id (assoc val :playing? (source-playing? (:source val)))])) m)))))))

(defn request-new-ticket
  "Request a new ticket for a playable source"
  [data type]
  (update-tickets)
  (dosync
   (if (= (count @ticket-map) max-sources)
     (let [id (get-lowest-priority)
           t (id @ticket-map)
           s (:source t)]
       (alter ticket-map dissoc id)
       (let [a (agent nil)]
         (send-off a (fn [_] (a/stop-source s)))) ; trick for IO in transaction
       (let [ticket (spawn-ticket s data type)]
         (alter ticket-map assoc (:id ticket) ticket)
         (:id ticket)))
     (let [s (first (clojure.set/difference @source-list (into #{} (map (fn [[id val]] (:source val)) @ticket-map))))
           ticket (spawn-ticket s data type)]
       (alter ticket-map assoc (:id ticket) ticket)
       (:id ticket)))))

(defn init-sources
  "Initialize the sources in the audio engine."
  []
  (dotimes [_ max-sources]
    (let [b (BufferUtils/createIntBuffer 1)]
      (a/generate-sources b)
      (assert-no-error (format "Unable to generate source."))
      (dosync
       (alter source-list conj (.get b 0))))))

(defn set-listener-data
  "High level function to tweak parameters of the listener."
  [data]
  (doall (map (fn [[pname value]]
                (a/set-listener-value (pname a/properties) value)
                (assert-no-error (format "Unable to set %s property for listener." pname))) data)))

(defn active?
  "Test if the current ticket is still valid or it has been recycled."
  [ticket]
  (contains? @ticket-map ticket))

(defn init-audio
  "Initialize the audio engine."
  [listener-data]
  (a/create-audio)
  (set-listener-data (merge default-listener listener-data))
  (init-sources))

(defn bgm-playing?
  "Test if a given bgm is playing or not."
  [id]
  (->> @ticket-map
       (filter #(and (= id (:buffer (val %)))
                     (= :bgm (:type (val %)))
                     (:playing? (val %))))
       ((complement empty?))))

(defn act-on-ticket
  "Wrapper for common sound-based operations."
  [ticket f state]
  (dosync
   (when (active? ticket)
     (alter ticket-map assoc-in [ticket :playing?] state)
     (f (:source (@ticket-map ticket))))))

(defn play
  "Play specific ticket."
  [ticket]
  (act-on-ticket ticket a/play-source true))

(defn stop
  "Stop specific ticket."
  [ticket]
  (act-on-ticket ticket a/stop-source false))

(defn pause
  "Pause specific ticket."
  [ticket]
  (act-on-ticket ticket a/pause-source false))

(defn rewind
  "Rewind specific ticket."
  [ticket]
  (act-on-ticket ticket a/rewind-source false))

(defn play-bgm
  "Obtain a ticket and play bgm, indefinitely."
  [buffer]
  (when-not (bgm-playing? buffer)
    (play (request-new-ticket {:buffer buffer :looping? true} :bgm))))

(defn remove-bgm
  "Remove bgm from source list."
  [buffer]
  (dosync
   (->> @ticket-map
        (filter #(and (= buffer (:buffer (val %))) (= :bgm (:type (val %)))))
        first
        first
        (alter ticket-map dissoc))))

(defn stop-bgm
  "Stop a specific bgm from playing."
  [buffer]
  (when (bgm-playing? buffer)
    (->> @ticket-map
         (filter #(and (= buffer (:buffer (val %))) (= :bgm (:type (val %))) (:playing? (val %))))
         first
         first
         stop)
    ; clean-up, free soure from ticket-map because a stopped bgm is dead
    (remove-bgm buffer)))

(defn play-sfx
  "Play a fire-and-forget sfx"
  ([buffer] (play-sfx buffer {}))
  ([buffer data]
   (play (request-new-ticket (assoc data :buffer buffer :looping? false) :soundfx))))
