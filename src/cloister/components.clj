(ns cloister.components
  (:require [cloister.utils :as utils]))

; In Cloister there are components and simple tags for
; entities. A Component is a more complex data structure
; that embodies a map embedded inside the entity.
; It is defined with the (defcomponent ) macro and takes
; a name (which will become a keyword for that component
; in the entity's map) and a vector of attributes +
; default values.

; A tag is a much simpler data structure, it is a
; keyword paired with a value for an entity.

(defmacro defcomponent
  "Macro for defining a new component type."
  [name bindings]
  (let [gen-name (gensym (str name))
        key-name (keyword name)
        ctor (symbol (str (clojure.core/name gen-name) "."))
        init-params (take-nth 2 (rest bindings))]
    `(do
       (defrecord ~gen-name ~(vec (take-nth 2 bindings)))
       (defn ~name
         [& rest#]
         {~key-name (reduce into (~ctor ~@init-params) rest#)}))))


(defn add-tag
  "Add a tag to the given entity."
  [e name value]
  (utils/deep-merge e {name value}))

(defn rem-tag
  "Remove a tag from the given entity."
  [e name]
  (dissoc e name))

;
; ======================== COMPONENTS ==================
;

(def entity-base
  {:init nil
   :destroy nil })

(defcomponent position
  [x 0
   y 0])

(defcomponent speed
  [x 0
   y 0])

(defcomponent update
  [fn nil
   always? false])

(defcomponent render
  [fn nil
   always? false
   z-index 0])

(defcomponent input
  [fn nil
   map #{}
   func-map {}
   always? false])

(defcomponent mouse
  [fn nil
   always? false])

; example
;(entity
; entity-base
; (position
;  {:x 100, :y 46})
; (speed
;  {:x 1.5, :y 0}))
