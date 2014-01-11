(ns cloister.bindings.audio
  (:import (org.lwjgl.openal AL10
                             AL
                             ALC10)))

(defn create-audio
  "Mapping for AL.create()"
  ([] (AL/create))
  ([args freq refresh sync] (AL/create args freq refresh sync))
  ([args freq refresh sync dev] (AL/create args freq refresh sync dev)))


(defn destroy-audio
  "Mapping for AL.destroy()"
  []
  (AL/destroy))

(defn get-device
  "Mapping for AL.getDevice()"
  []
  (AL/getDevice))

(defn get-context
  "Mapping for AL.getContext()"
  []
  (AL/getContext))

(defn created?
  "Mapping for AL.isCreated()"
  []
  (AL/isCreated))

(defn get-error
  "Mapping for AL10.getError()"
  []
  (AL10/alGetError))

(defn open-device
  "Mapping for ALC10.alcOpenDevice"
  ([] (open-device nil))
  ([s] (ALC10/alcOpenDevice s)))

(defn create-context
  "Mapping for ALC10.alcCreateContext"
  ([dev] (create-context dev nil))
  ([dev attr] (ALC10/alcCreateContext dev attr)))

(defn set-listener-value
  "Mapping for various AL10.alListener*()"
  [pname value]
   (cond
    (float? value) (AL10/alListenerf pname value)
    (instance? Boolean value) (AL10/alListeneri pname (if value AL10/AL_TRUE AL10/AL_FALSE))
    (integer? value) (AL10/alListeneri pname value)
    (sequential? value) (AL10/alListener3f pname (nth value 0) (nth value 1)  (nth value 2))
    :else
     (AL10/alListener pname value)))

(defn set-source-value
  "Mapping for various AL10.alSource*()"
  [source pname value]
  (cond
   (float? value) (AL10/alSourcef source pname value)
   (instance? Boolean value) (AL10/alSourcei source pname (if value AL10/AL_TRUE AL10/AL_FALSE))
   (integer? value) (AL10/alSourcei source pname value)
   (sequential? value) (AL10/alSource3f source pname (nth value 0) (nth value 1) (nth value 2))
   :else (AL10/alSource source pname value)))

(defn get-source-value
  "Mapping for various AL10.alGetSource*"
  ([source pname data]
   (cond
    (= :float data) (AL10/alGetSourcef source pname)
    (= :int data) (AL10/alGetSourcei source pname)
    :else (AL10/alGetSource source pname data))))

(defn get-listener-value
  "Mapping for various AL10.alGetListener*"
  ([pname data]
   (cond
    (= :float data) (AL10/alGetListenerf pname)
    (= :int data) (AL10/alGetListeneri pname)
    :else (AL10/alGetListener pname data))))

(defn generate-buffers
  "Mapping for AL10.alGenBuffers()"
  [buf]
  (AL10/alGenBuffers buf))

(defn delete-buffers
  "Mapping for AL10.alDeleteBuffers()"
  [b]
  (AL10/alDeleteBuffers b))

(defn generate-sources
  "Mapping for AL10.alGenSources()"
  [sources]
  (AL10/alGenSources sources))

(def no-error AL10/AL_NO_ERROR)

(defn make-bool
  "Truthify the AL_TRUE/AL_FALSE state"
  [value]
  ({ AL10/AL_TRUE true
     AL10/AL_FALSE false
   } value))

(def properties { :buffer AL10/AL_BUFFER
                  :looping? AL10/AL_LOOPING
                  :source-relative AL10/AL_SOURCE_RELATIVE
                  :source-state AL10/AL_SOURCE_STATE
                  :pitch AL10/AL_PITCH
                  :gain AL10/AL_GAIN
                  :min-gain AL10/AL_MIN_GAIN
                  :max-gain AL10/AL_MAX_GAIN
                  :max-distance AL10/AL_MAX_DISTANCE
                  :rolloff-factor AL10/AL_ROLLOFF_FACTOR
                  :cone-outer-gain AL10/AL_CONE_OUTER_GAIN
                  :cone-inner-angle AL10/AL_CONE_INNER_ANGLE
                  :cone-outer-angle AL10/AL_CONE_OUTER_ANGLE
                  :reference-distane AL10/AL_REFERENCE_DISTANCE
                  :position AL10/AL_POSITION
                  :velocity AL10/AL_VELOCITY
                  :direction AL10/AL_DIRECTION
                  :orientation AL10/AL_ORIENTATION
                 })

(def state-map { :initial AL10/AL_INITIAL
                 :paused AL10/AL_PAUSED
                 :playing AL10/AL_PLAYING
                })

(defn buffer-data
  "Mapping for AL10.alBufferData()"
  [buffer format data freq]
  (AL10/alBufferData buffer format data freq))

(defn play-source
  "Mapping for AL10/alSourcePlay"
  [s]
  (AL10/alSourcePlay s))

(defn stop-source
  "Mapping for AL10/alSourceStop"
  [s]
  (println "Stop called " s)
  (AL10/alSourceStop s))

(defn pause-source
  "Mapping for AL10/alSourcePause"
  [s]
  (AL10/alSourcePause s))

(defn rewind-source
  "Mapping for AL10/alSourceRewind"
  [s]
  (AL10/alSourceRewind s))
