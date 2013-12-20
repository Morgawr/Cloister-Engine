(ns cloister.core
  (:use cloister.bindings.input)
  (:use cloister.bindings.graphics)
  (:use cloister.sound)
  (:import (org.lwjgl.input Keyboard))
  (:import (org.lwjgl Sys))
  (:import (org.lwjgl.opengl Display
                             DisplayMode
                             GL11)))

; List of agents should be an atom with different screen depths
; There is going to be a query retrieving all the agents with X component
; on all screens or on just the current agent's screen

; A screen is a list of directives to spawn agents on the top-most layer
; Pop screen runs the de-initializer on all the agents of the top-most layer
; How to make sure agents shut down properly?
; - The atom's state is popped out of the top-most screen so update/render
; don't work on the old agents anymore but each agent is issued a shutdown
; event message.
; How to make agents communicate with each other for graceful synchronized
; shutdown?
; - Not possible, should it be possible? :|

; To selectively render agents there must be a function that tests if the
; agent is currently on the topmost screen or not, how to do this?
; SOLUTION: put agents inside sets and then test for the existence of
; the current agent inside the top-most set.

; Agents specify their own rendering function.
; QUESTION: Should the function have a selective explicit render for
; top-most screen rendering of agents or should it be implicit and
; specified by the agent?
; SOLUTION 1: have rendering function test with a filter before
; continuing
; SOLUTION 2: specify flag inside the renderable agent
; - solution #1 can be inside #2 with more coding so #2 is
; easier, gives more freedom and detracts nothing from the
; casual developer. #2 wins
; Probably do the same for update function too

; Sometimes high-performance/response operations are required for agents
; like collision detection and response, do we really need to use a clojure
; agent to perform this? Is the queued event going to impact performance/response?
; Why not store a dual element with agent+atom properties?
; Property 1: (agent) guaranteeing that the order of operations performed on the data
; structure are independent, aynchronous and properly ordered.
; Property 2: (atom) guaranteeing that the operations are atomic, consistent and
; immediate (within the limits of STM)
; How to get both?
; SOLUTION 1 - Store an atom inside the agent
; How to make it transparent for queries?
; SOLUTION 2 - Actually write agents on top of core.async using priority queues
; #2 is sexy as fuck

; Each entity should have the following:
; - init function
; - destroy function
; - update function (if update-able)
; - render function (if render-able)
; - render-if-not-on-top/update-if-not-on-top

; When adding and removing a screen we should be able to notify all the entities on the (former/newer)
; top-most screen that such event happened. This means that entities should have a :screen-added and
; :screen-removed tags that map to functions if they want to listen to such events.
; TODO

(defn stacktrace->string
  "Convert stacktrace from exception to string."
  [trace]
  (let [error (java.io.StringWriter. )]
    (.printStackTrace trace (java.io.PrintWriter. error))
    (.toString error)))

; This is the global error handling function, it's defined as an atom
; so it can be modified by users at runtime if necessary.
(def ERROR_HANDLER (atom
                    (fn [the-agent exception]
                      (.write *err* (str the-agent " (ID = " (:id @@the-agent) ") threw an exception.\n"))
                      (.write *err* (str (.getMessage exception) "\n"))
                      (.write *err* (str @@the-agent "\n"))
                      (.write *err* (stacktrace->string exception)))))


(defn set-entity-error-handler!
  "Set a new global error handler for exceptions in entities."
  [f]
  (reset! ERROR_HANDLER f))

(defmacro entity
  "Return a new entity with the given data components applied to it."
  [data]
  `(agent (atom (assoc ~data :id (get-next-id)))
          :error-mode :continue
          :error-handler #(@ERROR_HANDLER %1 %2)))

(defn e-send!
  "Asynchronously send a change of state to an entity, the action is performed inside the
  agent's own thread."
  [entity f & args]
  (apply send entity (fn [at func & args2] (apply swap! at func args2) at) f args))

(defn e-act!
  "Immediately operate on another entity's state, bypassing the event queue. (Privileged channel)"
  [entity f & args]
  (apply swap! @entity f args))

(defn has-tag?
  "Test if a given entity has the given tag."
  [entity tag]
  (contains? @@entity tag))

; Example of agents
; {
;
;   :screen-list [
;                  #{<entity whatev> <entity whatev> <entity whatev>} <-- 0th screen with entities living in it
;                  #{<entity whatev> <entity whatev> <entity whatev>} <-- 1st screen with entities living in it
;                  #{<entity whatev> <entity whatev> <entity whatev>} <-- 2nd screen with entities living in it
;                ]
;
;   :hud-list #{<entity whatev> <entity whatev> <entity whatev>} <-- privileged entity set for HUD and always-top-most stuff
; }
; for update, hud-list is called last
; for rendering, hud-list is called last

; Example of individual agent
; AGENT -> ATOM :
; { :init <FN>
;   :destroy <FN>
;   :update <FN>
;   :always-update? <bool>
;   :render <FN>
;   :always-render? <bool>
;   . . . other stuff
; }


(def CLOISTER_AGENTS (atom { :screen-list []
                             :hud-list #{} }))
(defn screen-list
  "Return the list of screens, abstracted from the atomicity."
  []
  (:screen-list @CLOISTER_AGENTS))

(defn hud-screen
  "Return the screen containing the hud entities."
  []
  (:hud-list @CLOISTER_AGENTS))

(def fps-accumulator (atom 0))
(def fps-last-time (atom 0))
(def time-accumulator (atom 0))
(def fps (atom 0))

(def ENTITY_ID (atom 0))

(defn get-next-id
  "Return unique id of an entity. Has side effects."
  []
  (swap! ENTITY_ID inc))

(defn get-time
  "Return the current system time in milliseconds."
  []
  (/ (* 1000 (Sys/getTime)) (Sys/getTimerResolution)))

(defn query-screen
  "Run a query of given tags paired to the given screen."
  [taglist screen]
  (filter #(every? true? (map (partial has-tag? %) taglist)) screen))

(defn query-topmost
  "Return a list of entities having all the tags in the taglist, from the top-most screen."
  [taglist]
    (query-screen taglist (first (screen-list))))

(defn query-hud
  "Return a list of entities having all the tags in the taglist, from the hud screen."
  [taglist]
  (query-screen taglist (hud-screen)))

(defn query-all
  "Return a set of entities having all the tags in the taglist, from all possible screens."
  [taglist]
  (let [all-entities (reduce into #{} (conj (screen-list) (hud-screen)))]
    (query-screen taglist all-entities)))

(defn spawn-entity!
  "Create a new entity, call its init function and append it to the top-most screen."
  [data & args]
  (let [e (entity (apply (:init data) data args))]
    (swap! CLOISTER_AGENTS update-in [:screen-list 0] conj e)))

(defn spawn-hud!
  "Create a new entity on the hud, call its init function and append it to the hud."
  [data]
  (let [e (entity ((:init data) data))]
    (swap! CLOISTER_AGENTS update-in [:hud-list] conj e)))

(defn destroy-entity!
  "Destroy the current entity, it must be called from inside the agent's own thread else
  unwanted consequences (like destroying the wrong entity) might happen."
  [state]
  (let [this *agent*]
    ((:destroy state) state)
    (if (contains? (hud-screen) this)
      (swap! CLOISTER_AGENTS update-in [:hud-list] disj this)
      (dotimes [n (count (screen-list))]
        (when (contains? (nth (screen-list) n) this)
          (swap! CLOISTER_AGENTS update-in [:screen-list n] disj this)))))) ; concurrency warning, possible race condition when creating/removing a screen!

(defn add-screen!
  "Create a new screen, add it to the list of screens and then initialize all the given entities."
  [to-add]
  (swap! CLOISTER_AGENTS update-in [:screen-list] #(into [#{}] %))
  (doseq [e to-add]
    (spawn-entity! e)))

(defn pop-screen!
  "Remove the top-most screen from the list of screens and destroy all the entities."
  []
  (let [s (first (screen-list))]
    (swap! CLOISTER_AGENTS update-in [:screen-list] #(into [] (rest %)))
    (doseq [e s]
      (e-send! e destroy-entity!))))

(defn update-fps
  "Update the FPS, should be used and called internally, it is
  called automatically by the rendering thread."
  []
  (let [t (get-time)]
    (if (> (+ @time-accumulator (- t @fps-last-time)) 1000)
      (do
        (reset! fps @fps-accumulator)
        (reset! fps-accumulator 0)
        (reset! time-accumulator 0))
      (do
        (swap! fps-accumulator inc)
        (reset! time-accumulator (+ @time-accumulator (- t @fps-last-time)))))
    (reset! fps-last-time t))
  @fps)

(defn update-entities
  "Called by the update future, it tests all the entities and updates those that need to."
  []
  (let [entities (query-all [:update])
        first-screen (first (screen-list))]
    (doseq [e entities]
      (when (or (contains? first-screen e)
                (contains? (hud-screen) e)
                (:always-update? @@e))
        (e-send! e (:update @@e))))))

(defn render-entities
  "Called by the render future, it renders the entities in the proper order:
  normal screens, first screen and then hud."
  []
  (GL11/glClear GL11/GL_COLOR_BUFFER_BIT)
  (doseq [s (reverse (screen-list))
          e (sort-by :z-index (query-screen [:render] s))]
    (when (or (contains? (first (screen-list)) e)
              (:always-render? @@e))
      ((:render @@e) @@e)))
  (doseq [e (query-hud [:render])]
    ((:render @@e) @@e)))


; Input handling should take all entities with an input component. Entities should have
; an :input-map, an :input function and an :always-input? test flag, the dispatcher checks
; if the entity has :input, then if it's on the topmost screen or has :always-input? as true,
; then tests if the entity's :input-map is triggered by the current input state and
; at last calls the :input function for that entity.
;
; To avoid the mess that is lwjgl's event based input (laggy for repeated events)
; we need to come up with a more snappy and faster way to send input events to listeners.
; We keep a map of the last input state with key -> :down/:held/:up value.
; :down -> key was just pressed in this frame (this lets us ignore repeat presses in case we don't need them)
; :held -> key has been pressed for a while
; :up -> key has just been released
;
; This map has three states:
; 1) when it receives new key data from the event queue
; 2) when it has been processed and is being sent to the entities
; 3) when it's getting ready for the next input frame
;
; At #1 we merge the new input state, when true -> we check if key already exists,
; if it doesn't then it becomes :held. when false then it becomes :up.
; At #2 we make sure the state is consistent and the entities can safely read whatever
; data they want. IMPORTANT: have to send the value, not the atom!! Else a slow entity might
; process a newer state and skip an older one.
; At #3 we make sure to remove all the :up and turn all the :down into :held

(def CLOISTER_INPUT (atom {})) ; The input map


(defn get-new-input
  "Obtain new input state and map it properly to the key data."
  [input-state]
  (into input-state (for [[k v] (get-key-events)]
                      [k (cond
                          (false? v) :up
                          (complement (contains? input-state k)) :down
                          :else :held)])))

(defn remove-old-input
  "Map all the :down keys to :held and remove all the :up keys."
  [input-state]
  (into {} (for [[k v] (filter (complement #(= (second %) :up)) input-state)]
             [k (if (= :down v) :held v)])))

(defn dispatch-input
  "Take care of sending input events to the proper entities."
  [input-state input-time]
  (when-not (nil? (seq input-state))
    (doseq [s (reverse (screen-list))
            e (query-screen [:input] s)]
      (when (and (or (contains? (first (screen-list)) e)
                     (:always-input? @@e))
                 (not (nil? (select-keys input-state (:input-map @@e))))) ; there's at least one key that specfic entity is listening to
        (e-send! e (:input @@e)  (select-keys input-state (:input-map @@e)) input-time))))
    input-state)

(defn input-routine
  "Input listener + dispatcher + input mapper."
  [time]
  (->> @CLOISTER_INPUT
       (get-new-input)
       (#(dispatch-input % time))
       (remove-old-input)
       (reset! CLOISTER_INPUT)))

; For mouse input, it is simpler than keyboard input, just look for :mouse and send
; everything to those entities. Check for :always-mouse? and also for hud this time around.

(defn dispatch-mouse
  "Take care of sending mouse events to the proper entities."
  [input-time]
  (let [state (merge (get-mouse-state) (get-mouse-coords))]
    (when-not (nil? (seq state))
      (doseq [h (query-hud [:mouse])]
        (e-send! h (:mouse @@h) state input-time))
      (doseq [s (reverse (screen-list))
              e (query-screen [:mouse] s)]
          (when (or (contains? (first (screen-list)) e)
                    (:always-mouse? @@e))
            (e-send! e (:mouse @@e) state input-time)))))
  nil)

(defn start-engine
  "Spawn the rendering and updating tasks, effectively starting the engine."
  [data]
  (let [{:keys [screen-height
                screen-width
                vsync
                fullscreen
                window-title
                loader ; function that loads the different resources - TODO have a loading screen eventually?
                update-interval
                listener]} data
        close-request (atom false)
        finished (promise)
        render (future
                 (let [input-time (atom 0)]
                   (set-display-mode (DisplayMode. screen-width screen-height) fullscreen)
                   (set-title window-title)
                   (set-vsync vsync)
                   (create-display)
                   (init-audio listener)
                   (GL11/glEnable GL11/GL_TEXTURE_2D)
                   (GL11/glClearColor 0 0 0 0)
                   (GL11/glEnable GL11/GL_BLEND)
                   (GL11/glBlendFunc GL11/GL_SRC_ALPHA GL11/GL_ONE_MINUS_SRC_ALPHA)
                   (GL11/glViewport 0 0 screen-width screen-height)
                   (GL11/glMatrixMode GL11/GL_MODELVIEW)
                   (GL11/glMatrixMode GL11/GL_PROJECTION)
                   (GL11/glLoadIdentity)
                   (GL11/glOrtho 0 screen-width screen-height 0 1 -1)
                   (GL11/glMatrixMode GL11/GL_MODELVIEW)
                   (loader)
                   (reset! fps-last-time (get-time))
                   (reset! input-time (get-time))
                   (clojure.core/while (not (Display/isCloseRequested))
                                       (update-fps)
                                       (input-routine @input-time)
                                       (dispatch-mouse @input-time)
                                       (reset! input-time (get-time))
                                       (render-entities)
                                       (Display/update))
                   (destroy-display)
                   (reset! close-request true)))
        scheduler (future (clojure.core/while (not @close-request)
                                              (update-entities)
                                              (Thread/sleep update-interval))
                    (shutdown-agents)
                    (deliver finished true))]
    [render scheduler finished]))
    ; might want to setup the physics engine thread eventually
