(ns cloister.render
  (:import (org.newdawn.slick.opengl TextureLoader))
  (:import (org.newdawn.slick.util ResourceLoader))
  (:import (org.newdawn.slick Color))
  (:import (org.lwjgl.opengl GL11)))


; Global map with textures loaded into memory paired with keyword
(def CLOISTER_TEXTMAP (atom {}))

(defn load-texture!
  "Load a new texture into memory. Has side effects."
  [id file fmt]
  (when (nil? (id @CLOISTER_TEXTMAP))
    (swap! CLOISTER_TEXTMAP assoc id (TextureLoader/getTexture fmt (ResourceLoader/getResourceAsStream file))))
  (id @CLOISTER_TEXTMAP))

(defn unload-texture!
  "Remove a texture from memory. Has side effects.
  WARNING: Make sure the texture is not used anymore before unloading it."
  [id]
  (let [text (id @CLOISTER_TEXTMAP)]
    (swap! CLOISTER_TEXTMAP dissoc id)
    (.release text)))

(defn get-texture
  "Obtain loaded texture from id."
  [id]
  (id @CLOISTER_TEXTMAP))

(defn render-at
  "Render given texture at given coordinates"
  [id {x :x y :y scale :scale}]
  (let [t (id @CLOISTER_TEXTMAP)
        w (* (.getTextureWidth t) scale)
        h (* (.getTextureHeight t) scale)]
    (.bind (Color/white))
    (.bind t)
    (GL11/glBegin GL11/GL_QUADS)
    (GL11/glTexCoord2f 0 0)
    (GL11/glVertex2f x y)
    (GL11/glTexCoord2f 1 0)
    (GL11/glVertex2f (+ x w) y)
    (GL11/glTexCoord2f 1 1)
    (GL11/glVertex2f (+ x w) (+ y h))
    (GL11/glTexCoord2f 0 1)
    (GL11/glVertex2f x (+ y h))
    (GL11/glEnd)))
