(ns cloister.bindings.graphics
  (:import (org.lwjgl.opengl Display)))

(defn display-sync
  "Mapping for Display.sync()"
  [amt]
  (Display/sync amt))

(defn create-display
  "Mapping for Display.create()"
  ([] (Display/create))
  ([a1 a2] (Display/create a1 a2))
  ([a1 a2 a3] (Display/create a1 a2 a3)))

(defn destroy-display
  "Mapping for Display.destroy()"
  []
  (Display/destroy))

(defn available-display-modes
  "Mapping for Display.getAvailableDisplayModes()"
  []
  (vec (Display/getAvailableDisplayModes)))

(defn display-mode
  "Mapping for Display.getDisplayMode()"
  []
  (Display/getDisplayMode))

(defn set-display-mode
  "Mapping for Display.setDisplayMode() and Display.setDisplayModeAndFullscreen()"
  [mode fscreen]
  (if fscreen
    (Display/setDisplayModeAndFullscreen mode)
    (Display/setDisplayMode mode)))

(defn set-fullscreen
  "Mapping for Display.setFullscreen()"
  [val]
  (Display/setFullscreen val))

(defn configure-display
  "Mapping for Display.setDisplayConfiguration()"
  [gamma brightness contrast]
  (Display/setDisplayConfiguration gamma brightness contrast))

(defn get-drawable
  "Mapping for Display.getDrawable()"
  []
  (Display/getDrawable))

(defn display-size
  "Return map with :height and :width of current display or the specific dimension given in the parameter."
  ([] { :width (Display/getWidth) :height (Display/getHeight)})
  ([key] (key { :width (Display/getWidth) :height (Display/getHeight) })))

(defn set-title
  "Mapping for Display.setTitle()"
  [title]
  (Display/setTitle title))

(defn get-title
  "Mapping for Display.getTitle()"
  []
  (Display/getTitle))

(defn set-vsync
  "Mapping for Display.setVSyncEnabled()"
  [val]
  (Display/setVSyncEnabled val))

(defn fullscreen?
  "Mapping for Display.isFullscreen()"
  []
  (Display/isFullscreen))

(defn resizable?
  "Mapping for Display.isResizable()"
  []
  (Display/isResizable))

(defn visible?
  "Mapping for Dispay.isVisible()"
  []
  (Display/isVisible))

(defn current?
  "Mapping for Display.isCurrent()"
  []
  (Display/isCurrent))

(defn update-display
  "Mapping for Display.update()"
  []
  (Display/update))
