(defproject cloister "0.1.0-SNAPSHOT"
  :description "Cloister Core is a fully multithreaded game engine."
  :url "none yet"
  :license {:name "Modified zlib"
            :url "none yet"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [lein-light-nrepl "0.0.6"]
                 [org.lwjgl.lwjgl/lwjgl "2.9.0"]
                 [org.lwjgl.lwjgl/lwjgl_util "2.9.0"]
                 [slick-util "1.0.0"]]
                 ;[slick-util "0.1.0"]]
  ;:repositories { "local" ~(str (.toURI (java.io.File. "local_maven")))}
  ;:local-repo "local_maven"
  :jvm-opts [~(str "-Djava.library.path=native/:" (System/getProperty "java.library.path"))]
  :plugins [[lein-marginalia "0.7.1"]]
  :repl-options { :nrepl-middleware [lighttable.nrepl.handler/lighttable-ops] } )
