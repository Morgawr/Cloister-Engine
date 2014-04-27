(ns cloister.physics
  (:require [cloister.bindings.dyn4j :as dyn])
  (:use [cloister.utils :only [flush!]]))

(def NANO_TO_BASE 1.0e9)

; Physics are tied with the dyn4j engine, the dyn4j engine
; assumes the existence of a world object that contains
; entities with relevant physical data and every iteration
; will step through the given entities and simulate their
; movements given a delta-time.

; Ideally, cloister agents should initialize their physics
; component upon initialization and add themselves to the
; physics world. From that moment on, they start to exist
; and live in the physical world.

; For other components to retrieve physical data (like
; position for rendering) or even interact with forces
; and other physics-related there will be functions used
; to retrieve and insert state in the physics world.

; Each component registers its own physical counterpart
; (if necessary) and is given a physical identifier
; (a ticket, similar to the audio engine). With this
; ticket then it will be possible to query the physics
; world to perform operations on it.


; -- INTERFACE ----
;
; The engine holds the following state:
;   - WORLD entity
;   - world settings
;   - ticket list/queue for external entities
;
; The engine should expose the following functions:
;    - update function to be sent to the physics thread <-- this is not how it works
;    - ad-hoc query functions for entities that given a ticket
;        are able to retrieve (or update) requested data
;        in a fully threadsafe manner <-- this is hard, using operation-queue for it
;    - functions to add/remove entities by ticketing system

; Due to the tricky nature of a non-threadsafe physics engine, we need to
; specify a proper way to accessing resources without incurring in
; race conditions. This is done by exposing an epochal public interface,
; as is usual for Clojure standards, and we let the physics thread handle
; the exposition of data in a thread safe manner. The physics thread
; unfortunately will have to export/populate a full state-map every iteration
; with all the exported public attributes (And the cloister-physmap for our
; physical entities). This is slower than preferred but I see no clear alternative
; yet, it should still be fast enough considering there is a full core available
; for physics which by itself is overkill.

; Dictionary containing all currently enabled tickets for
; physics entities in the game world
(def CLOISTER_PHYSMAP (atom {}))

; Ticket generator, always giving unique IDs
(def ticket-gen (atom 0))

; Physics entities are flgged as not-active until the physics
; thread actually adds them to the world instance. Entities should
; always check for active before acting on their physics component.
(def ticket-base
  {
   :id 0
   :active? false
   :state {}
  })

(def CLOISTER_WORLD (dyn/create-world))

; This is a special queue which threads can use to submit
; operations on the world's state.
; For example it's possible to change the gravity of the
; world by passing a closure on the set-gravity function.
(def operation-queue (atom []))

(defn run-physics
  "Actual running core inside the physics thread."
  [s finished]
  (let [settings (dyn/create-settings (:settings s))
        bounds (:bounds s) ; this is a { :width a :height b} map
        action-fn (fn [world fun]
                    (fun world))]
    (dyn/set-world-settings CLOISTER_WORLD settings)
    (when-not (nil? bounds)
      (dyn/set-world-bounds CLOISTER_WORLD bounds))
    (let [last-time (atom (System/nanoTime))]
      (while (not (realized? finished))
        (let [todo (flush! operation-queue [])]
          (when (seq todo)
            (reduce action-fn world todo)))
        (.updatev CLOISTER_WORLD (/ (double (- (System/nanoTime) @last-time)) NANO_TO_BASE))
        ; TODO - step to update CLOISTER_PHYSMAP
        (reset! last-time (System/nanoTime))
        (Thread/sleep 16))))) ; TODO - this sleep interval should be in the global engine settings

(defn start-physics
  "Called from the game's core. It's effectively a standalone
  thread running inside the game's core."
  [data finished]
  (let [phys-settings (:phys-settings data)
        enabled? (:phys-enabled? data)]
    (when enabled?
      (future
        (try
          (run-physics phys-settings finished)
          (catch Exception e
            (println (.printStackTrace e))))))))


; Physics entity data format, this is just a temporary example
(def example-format
  {
   :shape { ; data related to the specific body shape
           :type :whatever
           :parameter1 :value1
           :parameter2 :value2
           ; etc
           }
   :fixture { ; data related to the body :fixture
             :parameter1 :value1
             :parameter2 :value2
             :parameter3 :value3
             ; etc
             }
   :body { ; additional body parameters
          :parameter1 :value1
          :parameter2 :value2
          ; etc
          }
   :mass 0}) ; This type of data will also be stored in the ticket map,
             ; the physics world will take care of copying all related
             ; data over after/before every update.
             ; TODO - make this potentially less expensive, do a diff?

; Adding a joint
(def example-joint
  {
   :body1 :id1
   :body2 :id2
   :parameter1 :value1
   :parameter2 :value2
   ;etc
   })

