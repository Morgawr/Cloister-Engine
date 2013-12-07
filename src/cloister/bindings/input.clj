(ns cloister.bindings.input
  (:require [cloister.bindings.graphics :as g])
  (:import (org.lwjgl.input Keyboard
                            Mouse)))

; This list is incomplete because I was too tired/bored to write all of it.
; Eventually going to add keys as I need them.
(def keyb { :0 Keyboard/KEY_0
            :1 Keyboard/KEY_1
            :2 Keyboard/KEY_2
            :3 Keyboard/KEY_3
            :4 Keyboard/KEY_4
            :5 Keyboard/KEY_5
            :6 Keyboard/KEY_6
            :7 Keyboard/KEY_7
            :8 Keyboard/KEY_8
            :9 Keyboard/KEY_9
            :a Keyboard/KEY_A
            :b Keyboard/KEY_B
            :c Keyboard/KEY_C
            :d Keyboard/KEY_D
            :e Keyboard/KEY_E
            :f Keyboard/KEY_F
            :g Keyboard/KEY_G
            :h Keyboard/KEY_H
            :i Keyboard/KEY_I
            :j Keyboard/KEY_J
            :k Keyboard/KEY_K
            :l Keyboard/KEY_L
            :m Keyboard/KEY_M
            :n Keyboard/KEY_N
            :o Keyboard/KEY_O
            :p Keyboard/KEY_P
            :q Keyboard/KEY_Q
            :r Keyboard/KEY_R
            :s Keyboard/KEY_S
            :t Keyboard/KEY_T
            :u Keyboard/KEY_U
            :v Keyboard/KEY_V
            :w Keyboard/KEY_W
            :x Keyboard/KEY_X
            :y Keyboard/KEY_Y
            :z Keyboard/KEY_Z
            :f1 Keyboard/KEY_F1
            :f2 Keyboard/KEY_F2
            :f3 Keyboard/KEY_F3
            :f4 Keyboard/KEY_F4
            :f5 Keyboard/KEY_F5
            :f6 Keyboard/KEY_F6
            :f7 Keyboard/KEY_F7
            :f8 Keyboard/KEY_F8
            :f9 Keyboard/KEY_F9
            :f10 Keyboard/KEY_F10
            :f11 Keyboard/KEY_F11
            :f12 Keyboard/KEY_F12
            :up Keyboard/KEY_UP
            :down Keyboard/KEY_DOWN
            :left Keyboard/KEY_LEFT
            :right Keyboard/KEY_RIGHT
            :home Keyboard/KEY_HOME
            :lctrl Keyboard/KEY_LCONTROL
            :rctrl Keyboard/KEY_RCONTROL
            :lalt Keyboard/KEY_LMETA
            :ralt Keyboard/KEY_RMETA
            :lshift Keyboard/KEY_LSHIFT
            :rshift Keyboard/KEY_RSHIFT
            :space Keyboard/KEY_SPACE
            :return Keyboard/KEY_RETURN
            })

(defn- reverse-map
  "Reverses a map of key->val to val->key."
  [m]
  (into {} (map (comp vec reverse) (into [] m))))

(defn kb->key
  "Goes from Keyboard/KEY_* to its related keyword."
  [KEY]
  ((reverse-map keyb) KEY))

(defn is-key-down?
  "Mapping for Keyboard.isKeyDown(), receives a keyword as input."
  [k]
  (Keyboard/isKeyDown (keyb k)))

(defn enable-repeat-events
  "Mapping for Keyboard.enableRepeatEvents()"
  [val]
  (Keyboard/enableRepeatEvents val))

(defn create-keyboard
  "Mapping for Keyboard.create()"
  []
  (Keyboard/create))

(defn destroy-keyboard
  "Mapping for Keyboard.destroy()"
  []
  (Keyboard/destroy))

(defn get-key-events
  "Returns a map of keypresses & state (true|false) for the current key queue."
  []
  (loop [continue? (Keyboard/next) states {}]
    (if-not continue?
      states
      (let [new-states (assoc states (kb->key (Keyboard/getEventKey))
                                     (Keyboard/getEventKeyState))]
        (recur (Keyboard/next) new-states)))))

(defn create-mouse
  "Mapping for Mouse.create()"
  []
  (Mouse/create))

(defn destroy-mouse
  "Mpping for Mouse.destroy()"
  []
  (Mouse/destroy))

(defn mouse-button-name
  "Mapping for Mouse.getButtonName()"
  [b]
  (Mouse/getButtonName b))

(defn mouse-button-count
  "Mapping for Mouse.getButtonCount()"
  []
  (Mouse/getButtonCount))

(defn get-mouse-state
  "Returns a map of mouse buttons & states (true|false) for the current mouse event queue."
  []
  (loop [continue? (Mouse/next) states {}]
    (if-not continue?
      states
      (let [new-states (assoc states (Mouse/getEventButton)
                         (Mouse/getEventButtonState))]
        (recur (Mouse/next) new-states)))))

(defn get-mouse-coords
  "Returns mouse coordinates and delta X/Y since last call."
  []
  (let [height (g/display-size :height)]
    (if-not (Mouse/isGrabbed)
      {:x (Mouse/getX) :y (- height (Mouse/getY))
       :dx (Mouse/getDX) :dy (Mouse/getDY)
       :dwheel (Mouse/getDWheel)
       }
      {:dx (Mouse/getX) :dy (Mouse/getY)
       :dwheel (Mouse/getDWheel)
       })))

(defn set-mouse-grabbed
  "Mapping for Mouse.setGrabbed()."
  [val]
  (Mouse/setGrabbed val))


(defn mouse-inside-window?
  "Mapping for Mouse.isInsideWindow."
  []
  (Mouse/isInsideWindow))

