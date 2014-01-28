(ns cloister.pong-test.core
  (:require [cloister.core :as c])
  (:require [cloister.bindings.graphics :as g])
  (:require [cloister.render :as r])
  (:require [cloister.sound :as s])
  (:require [cloister.utils :as utils])
  (:use [cloister.bindings.input])
  (:use [cloister.bindings.audio])
  (:import (org.lwjgl.opengl Display
                             DisplayMode
                             GL11))
  (:import (org.newdawn.slick.opengl TextureLoader))
  (:import (org.newdawn.slick.util ResourceLoader))
  (:import (org.newdawn.slick Color))
  (:import (org.lwjgl.input Mouse
                            Keyboard)))


(def base-speed { :x 0 :y 0})
(def max-speed 5)
(def paddle-speed 0.7)
(def base-position { :x 0 :y 0})


(defn loader
  []
  (r/load-texture! :ball "resources/ball.png" "PNG")
  (r/load-texture! :paddle "resources/paddle.png" "PNG")
  (s/load-sound! :pong "resources/pong_sounds/pong.wav"))

(defn render-text
  [{:keys [position texture]}]
  (r/render-at texture { :x (:x position)
                         :y (:y position)
                         :scale 1}))

(defn reset-ball
  [state]
  (let [sign (- (rand-int 10) 5)
        x-speed (* (if (zero? sign) 1 (/ sign (Math/abs sign))) (+ 3 (rand-int (- max-speed 2))))]
    (assoc state :position (assoc base-position :x (- (/ 1280 2) 8)
                                                :y (rand-int 700))
                 :speed (assoc base-speed :x x-speed
                                          :y (- (rand-int (* 2 max-speed)) max-speed)))))

(defn colliding?
  [ball paddle]
  (let [x1 (get-in ball [:position :x])
        y1 (get-in ball [:position :y])
        x2 (get-in paddle [:position :x])
        y2 (get-in paddle [:position :y])
        w2 16
        h2 128]
    (and (< x2 x1 (+ x2 w2))
         (< y2 y1 (+ y2 h2)))))


(defn check-paddle-collision
  [{:keys [position speed] :as state} delta]
  (let [x (:x position)
        y (:y position)
        s-x (:x speed)
        s-y (:y speed)
        paddles (doall (map (comp deref deref) (filter #(or (= (:type @@%) :player1)
                                                            (= (:type @@%) :player2))
                                           (c/query-topmost [:type]))))
        newstate (assoc state :position { :x (+ x (* delta s-x))
                                          :y (+ y (* delta s-y))})]
    (if-not (empty? (doall (filter #(colliding? newstate %) paddles)))
      (do
        (s/play-sfx :pong)
        [(- s-x) s-y])
        [s-x s-y])))

(defn move-ball
  [state delta]
  (let [s-x (get-in state [:speed :x])
        s-y (get-in state [:speed :y])
        x (get-in state [:position :x])
        y (get-in state [:position :y])
        nsx (if-not (< 0 (+ x (* delta s-x)) (- 1280 16)) (- s-x) s-x)
        nsy (if-not (< 0 (+ y (* delta s-y)) (- 720 16)) (- s-y) s-y)]
    (when-not (= nsy s-y)
      (s/play-sfx :pong))
    (if-not (= nsx s-x)
      ((:reset-ball state) state)
      (let [[new-x new-y] (check-paddle-collision (assoc state :speed {:x nsx :y nsy}) delta)]
        (assoc state :speed {:x new-x :y new-y}
                     :position { :x (+ x (* delta new-x))
                                 :y (+ y (* delta new-y))})))))

(defn update-ball
  [state]
  (let [cur-time (c/get-time)
        delta (- cur-time (:last-update state))]
    (-> state
        (move-ball (float (/ delta 15)))
        (assoc :last-update cur-time))))

(def init-data
  {:screen-width 1280
   :screen-height 720
   :vsync true
   :fullscreen false
   :window-title "Pong example"
   :loader loader
   :update-interval 60
   :listener {}})

; TODO - have an entity-base so it's easier to write stuff
(def ball
  {:reset-ball #(reset-ball %)
   :init (fn [data]
           (-> data
               ((:reset-ball ball))
               (assoc :last-update (c/get-time))))
   :destroy (fn [_] nil)
   :texture :ball
   :render {:fn #(render-text %)
            :render? false
            :z-index 1}
   :position base-position
   :speed base-speed
   :update {:fn #(update-ball %)
            :always? false}
   :last-update 0})

(defn init-paddle
  "Data is the base entity data, type is either :player1 or :player2"
  [data type]
  (if (= :player2 type)
    (utils/deep-merge data
                      {:type type
                       :input {:map #{:up :down}}
                       :position { :x (- 1280 20 16) :y 200}})
    (assoc data :position { :x 20 :y 200})))

(defn move-up
  [state press time]
  (let [y (get-in state [:position :y])
        delta (- (c/get-time) time)]
    (if (and (not (= :up press))
             (> (- y (* delta paddle-speed)) 5))
      (assoc-in state [:position :y] (- y (* delta paddle-speed)))
      state)))

(defn move-down
  [state press time]
  (let [y (get-in state [:position :y])
        delta (- (c/get-time) time)]
    (if (and (not (= :up press))
             (< (+ y (* delta paddle-speed)) (- 720 133)))
      (assoc-in state [:position :y] (+ y (* delta paddle-speed)))
      state)))

(def paddle
  {:init init-paddle
   :destroy (fn [_] nil)
   :texture :paddle
   :render {:fn #(render-text %)
            :z-index 2
            :always? false}
   :position base-position ; this will depend on type
   :type :player1
   :input {:map #{:w :s} ; W - S for player 1 up/down
           :func-map { :w (partial move-up)
                       :s (partial move-down)
                       :up (partial move-up)
                       :down (partial move-down)}
           :always? false
           :fn (fn [state input time] ; TODO refactor this into a utility function because wtf so complex
                 (let [{:keys [func-map]} (:input state)]
                   (reduce #(((first %2) func-map) %1 (second %2) time)
                           state (select-keys input (keys func-map)))))}
   })

(defn -main []
  (let [[render scheduler done?] (c/start-engine init-data)]
    (c/add-screen! [[paddle [:player1]][paddle [:player2]][ball]])
    @done?))

(-main)

;(def a (c/start-engine init-data))

;(s/play-bgm :pong)
;(c/pop-screen!)
;c/CLOISTER_AGENTS
;(c/spawn-entity! sounder)
;(c/spawn-entity! ball)
;(c/spawn-entity! paddle :player2)

