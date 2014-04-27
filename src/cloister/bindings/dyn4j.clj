(ns cloister.bindings.dyn4j
  (:import (org.dyn4j.dynamics World
                               Settings)))

(defn create-world
  "Mapping for World constructor"
  ([] (World.)))

(defn set-world-settings
  "Mapping for World.setSettings"
  [world settings]
  (.setSettings world settings))

(defn get-world-settings
  "Mapping for World.getSettings"
  [world]
  (.getSettings world))

(defn give-nil [& rest]
  nil)

(def keyword->settings
  { :angular-tolerance {:set #(.setAngularTolerance %1 %2)
                        :get #(.getAngularTolerance %1)}
    :autosleeping-enabled {:set #(.setAutosleepingEnabled %1 %2)
                           :get #(.isAutosleepingEnabled %1)}
    :baumgarte {:set #(.setBaumgarte %1 %2)
                :get #(.getBaumgarte %1)}
    :continuous-detection-mode {:set #(.setContinuousDetectionMode %1 %2)
                                :get #(.getContinuousDetectionMode %1)}
    :linear-tolerance {:set #(.setLinearTolerance %1 %2)
                       :get #(.getLinearTolerance %1)}
    :maximum-angular-correction {:set #(.setMaximumAngularCorretion %1 %2)
                                 :get #(.getMaximumAngularCorrection %1)}
    :maximum-linear-correction {:set #(.setMaximumLinearCorrection %1 %2)
                                :get #(.getMaximumLinearCorrection %1)}
    :maximum-rotation {:set #(.setMaximumRotation %1 %2)
                       :get #(.getMaximumRotation %1)}
    :maximum-translation {:set #(.setMaximumTranslation %1 %2)
                          :get #(.getMaximumTranslation %1)}
    :position-constraint-solver-iterations {:set #(.setPositionConstraintSolverIterations %1 %2)
                                            :get #(.getPositionConstraintSolverIterations %1)}
    :restitution-velocity {:set #(.setRestitutionVelocity %1 %2)
                           :get #(.getRestitutionVelocity %1)}
    :sleep-angular-velocity {:set #(.setSleepAngularVelocity %1 %2)
                             :get #(.getSleepAngularVelocity %1)}
    :sleep-linear-velocity {:set #(.setSleepLinearVelocity %1 %2)
                            :get #(.getSleepLinearVelocity %1)}
    :sleep-time {:set #(.setSleepTime %1 %2)
                 :get #(.getSleepTime %1)}
    :step-frequency {:set #(.setStepFrequency %1 %2)
                     :get #(.getStepFrequency %1)}
    :velocity-constraint-solver-iterations {:set #(.setVelocityConstraintSolverIterations %1 %2)
                                            :get #(.getVelocityConstraintSolverIterations %1)}
    :warm-start-distance {:set #(.setWarmStartDistance %1 %2)
                          :get #(.getWarmStartDistance %1)}
    :angular-tolerance-squared {:set give-nil
                                :get #(.getAngularToleranceSquared %1)}
    :linear-tolerance-squared {:set give-nil
                               :get #(.getLinearToleranceSquared %1)}
    :maximum-angular-correction-squared {:set give-nil
                                         :get #(.getMaximumAngularCorrectionSquared %1)}
    :maximum-linear-correction-squared {:set give-nil
                                        :get #(.getMaximumLinearCorrectionSquared %1)}
    :maximum-rotation-squared {:set give-nil
                               :get #(.getMaximumRotationSquared %1)}
    :maximum-translation-squared {:set give-nil
                                  :get #(.getMaximumTranslationSquared %1)}
    :restitution-velocity-squared {:set give-nil
                                   :get #(.getRestitutionVelocitySquared %1)}
    :sleep-angular-velocity-squared {:set give-nil
                                     :get #(.getSleepAngularVelocitySquared %1)}
    :sleep-linear-velocity-squared {:set give-nil
                                    :get #(.getSleepLinearVelocitySquared %1)}
    :warm-start-distance-squared {:set give-nil
                                  :get #(.getWarmStartDistanceSquared %1)}
    })

(defn create-settings
  "Take a map of settings (key->value) and return a new
  instance of dyn4j.dynamics.Settings"
  [setting-map]
  (let [s (Settings.)]
    (doseq [[key val] setting-map]
      ((:set (keyword->settings key)) s val))
    s))
