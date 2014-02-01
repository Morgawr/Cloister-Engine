(ns font-test.core
  (:require [cloister.core :as c])
  (:require [cloister.bindings.graphics :as g])
  (:require [cloister.render :as r])
  (:require [cloister.sound :as s])
  (:require [cloister.utils :as utils :refer [let-keys]])
  (:require [cloister.components :as comps])
  (:import (org.lwjgl.opengl Display
                             DisplayMode
                             GL11))
  (:import (org.newdawn.slick.opengl TextureLoader))
  (:import (org.newdawn.slick.util ResourceLoader))
  (:import (org.newdawn.slick Color))
  (:import (org.newdawn.slick TrueTypeFont))
  (:import (org.lwjgl.input Mouse
                            Keyboard)))

(def init-data
  { :screen-width 1280
    :screen-height 720
    :vsync true
    :fullscreen false
    :window-title "Font Example"
    :loader (fn []
              (r/load-font! :times-new-roman-bold "Times New Roman" [:bold] true 25 false)
              (r/load-font! :times-new-roman-italic "Times New Roman" [:italic] true 25 false)
              (r/load-font! :times-new-roman "Times New Roman" [] true 25 false))
    :update-interval 60
    :listener {}
   })

(comps/defcomponent render-string
  [color :white
   font :times-new-roman
   message ""])

(defn draw-text
  [state]
  (let-keys [[render-string position] state
             [font message color] render-string
             [x y] position]
    (r/render-string font message x y color)))

(defn create-text-entity
  [x y message color font]
  (c/entity
   comps/entity-base
   (comps/position {:x x :y y})
   (comps/render {:fn draw-text})
   (render-string {:color color
                   :message message
                   :font font})))

(defn -main []
  (let [[render scheduler done?] (c/start-engine init-data)] ; init our engine
    (c/add-screen! [[(create-text-entity 100 200 "Hello world" :yellow :times-new-roman-bold)]
                    [(create-text-entity 600 50 "This is a font rendering example" :green :times-new-roman)]])
    @done?))

(-main)
