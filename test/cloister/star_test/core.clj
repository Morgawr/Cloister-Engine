(ns cloister.star-test.core
  (:require [cloister.core :as c])
  (:require [cloister.bindings.graphics :as g])
  (:require [cloister.render :as r])
  (:require [cloister.sound :as s])
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


(defn loader
  []
  (r/load-texture! :star-yellow "resources/star.png" "PNG")
  (r/load-texture! :star-blue "resources/star2.png" "PNG")
  (r/load-texture! :star-green "resources/star3.png" "PNG"))


(def init-data
  {:screen-width 1280
   :screen-height 720
   :vsync true
   :fullscreen false
   :window-title "Star Test"
   :loader loader
   :update-interval 60
   :listener {}
   })

(defn render-star
  "Render function for the star entity."
  [{:keys [x y texture]}]
  (r/render-at texture {:x x :y y :scale 1}))

(defn flickering
  "Return a new random texture."
  []
  (condp = (rand-int 3)
    0 :star-yellow
    1 :star-blue
    2 :star-green))

; This is a base star entity, it doesn't do much
(def star-base
  { :init (fn [data]
            (assoc data
              :x (rand-int 1280)
              :y (rand-int 720)))
    :destroy (fn [_]
               (println "Star destroyed."))
    :texture :star-yellow ; default texture base
    :render (fn [state] ; base rendering function
              (render-star state))
    :always-render? false ; <-- this tests if we should render the entity even when it's not on the top screen
    :z-index 1
    :x 0
    :y 0
   })


; Specialization of the base star, this one flickers as it updates
(def flickering-star
  (assoc star-base
    :update #(assoc % :texture (flickering))
    :always-update? false))

; Specialization of the base star, this one is green and stays even under screens
(def permanent-star
  (assoc star-base
    :always-render? true ; <-- we make it permanent between screen transitions
    :texture :star-green))

(def permanent-flickering-star
  (merge flickering-star permanent-star {:always-update? true}))

(defn move-star
  "Move the star given the coordinates in step."
  [state step down? time]
  (let [{:keys [x y]} state
        {:keys [velx vely]} step
        delta (- (c/get-time) time)]
    (if-not (= down? :up)
      (assoc state :x (+ x (* delta velx)) :y (+ y (* delta vely)))
      state)))

(def moving-star
  (assoc star-base
    :input-map #{:w :a :s :d}
    :input-func { :w #(move-star %1 {:velx 0 :vely -1} %2 %3)
                  :a #(move-star %1 {:velx -1 :vely 0} %2 %3)
                  :s #(move-star %1 {:velx 0 :vely 1} %2 %3)
                  :d #(move-star %1 {:velx 1 :vely 0} %2 %3)}
    :always-input? false
    :z-index 0
    :input (fn [state input time]
             (let [{:keys [input-func]} state]
               (reduce #(((first %2) input-func) %1 (second %2) time)
                       state (select-keys input (keys input-func)))))))

(def spawnable-star
  (assoc star-base :init (fn [data x y]
                           (assoc data :x x :y y))))

(defn mouse-callback
  "Execute whatever when clicking."
  [state data time]
  (when (data 0)
    (c/spawn-entity! spawnable-star (:x data) (:y data)))
  state)

(def mouse-listener
    { :init identity
      :destroy identity
      :mouse mouse-callback
      :always-mouse? false })


(defn -main
  "We start here"
  []
  (let [[render scheduler done?] (c/start-engine init-data)] ; init our engine
    (c/add-screen! []) ; add a new screen to the engine, so we can hold entities
    (c/spawn-entity! mouse-listener) ; listen to mouse
    (c/spawn-entity! moving-star)
    (c/spawn-entity! flickering-star)
    (c/spawn-entity! flickering-star)
    (c/spawn-entity! flickering-star)
    @done?)) ; need this promise to block until it's over
(-main)
