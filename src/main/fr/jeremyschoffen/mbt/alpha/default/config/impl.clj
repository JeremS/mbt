(ns fr.jeremyschoffen.mbt.alpha.default.config.impl
  (:require
    [ubergraph.core :as graph]
    [ubergraph.alg :as graph-alg]
    [medley.core :as medley]
    [fr.jeremyschoffen.dolly.core :as dolly]
    [fr.jeremyschoffen.mbt.alpha.utils :as u]))


(defrecord Calc [f deps])

(defn calc? [x]
  (instance? Calc x))

(defn calc
  "Define a computation to be made in the config.

  args:
  - `f`: a function of 1 argument (the config map) that returns a value to be placed in the config.
  - `deps`: keywords declaring which keys of the config the computation depends on."
  [f & deps]
  (Calc. f deps))



(defn extract-calcs
  "Turns the config into a map int a format suitable to construct a dependency graph."
  [conf]
  (persistent!
    (reduce-kv
      (fn [acc k v]
        (if (calc? v)
          (assoc! acc k (:deps v))
          acc))
      (transient {})
      conf)))


(defn execution-plan
  "Compute the order in which keys must be computed."
  [g]
  (-> g graph-alg/topsort reverse))


(defn compute
  "Takes a config and compute the parts marked for computation.

  A config is a map from keys to values. Special values are made
  using the [[fr.jeremyschoffen.mbt.alpha.default.config.impl/calc]] function.
  It marks these values of the config map for computation."
  [conf]
  (let [calcs-map (extract-calcs conf)
        calcs (set (keys calcs-map))
        dependency-graph (graph/digraph calcs-map)
        plan (execution-plan dependency-graph)]
    (persistent!
      (reduce (fn [acc k]
                (if-let [computation (and (contains? calcs k)
                                          (-> acc k :f))]
                  (assoc! acc k (computation acc))
                  acc))
              (transient conf)
              plan))))
