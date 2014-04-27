(ns cloister.physics
  (:require [cloister.bindings.dyn4j :as dyn])

; This is just the first draft for a physics engine inside
; cloister. There's not much here yet aside from comments

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
;    - update function to be sent to the physics thread
;    - ad-hoc query functions for entities that given a ticket
;        are able to retrieve (or update) requested data
;        in a fully threadsafe manner <-- this is hard
;    - functions to add/remove entities by ticketing system
