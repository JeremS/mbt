(ns com.jeremyschoffen.mbt.alpha.building.deps
  (:require
    [clojure.spec.alpha :as s]
    [clojure.tools.deps.alpha.reader :as deps-reader]
    [com.jeremyschoffen.mbt.alpha.specs]
    [com.jeremyschoffen.mbt.alpha.utils :as u]))


(defn get-deps [{wd :project/working-dir}]
  (deps-reader/slurp-deps (u/safer-path wd "deps.edn")))

(u/spec-op get-deps
           (s/keys :req [:project/working-dir])
           :project/deps)



