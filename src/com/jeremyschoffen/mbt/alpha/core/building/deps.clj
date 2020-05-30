(ns com.jeremyschoffen.mbt.alpha.core.building.deps
  (:require
    [clojure.tools.deps.alpha.reader :as deps-reader]
    [com.jeremyschoffen.mbt.alpha.core.specs]
    [com.jeremyschoffen.mbt.alpha.core.utils :as u]))


(defn get-deps [{wd :project/working-dir}]
  (deps-reader/slurp-deps (u/safer-path wd "deps.edn")))

(u/spec-op get-deps
           :param {:req [:project/working-dir]}
           :ret :project/deps)
