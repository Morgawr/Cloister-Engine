(ns cloister.bindings.font
  (:import (org.newdawn.slick TrueTypeFont
                              Color))
  (:import (java.awt Font)))

(defn make-color
  "Mapping for Slick's Color()"
  ([r g b]
   (Color. r g b))
  ([r g b a]
   (Color. r g b a))
  ([value]
   (Color. value)))

(def color-map
  {:black Color/black
   :blue Color/blue
   :cyan Color/cyan
   :dark-gray Color/darkGray
   :gray Color/gray
   :green Color/green
   :light-gray Color/lightGray
   :magenta Color/magenta
   :orange Color/orange
   :pink Color/pink
   :red Color/red
   :white Color/white
   :yellow Color/yellow
   })

(def font-style-map
  {:bold Font/BOLD
   :italic Font/ITALIC
   :plain Font/PLAIN
   })

(defn draw-string
  "Mapping for Slick's TrueTypeFont drawString()"
  [font x y string color]
  (.drawString font x y string (if (keyword? color)
                                 (color color-map)
                                 color)))

(defn get-height
  "Mapping for Slick's TrueTypeFont getHeight()"
  ([font] (.getHeight font))
  ([font string] (.getHeight font string)))

(defn get-width
  "Mapping for Slick's TrueTypeFont getWidth()"
  ([font] (.getWidth font))
  ([font string] (.getWidth font string)))

(defn map-style
  "Convert a vector of styles into its relative flag combination"
  [flags]
  (apply bit-or (map font-style-map flags)))
