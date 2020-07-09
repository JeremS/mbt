(ns com.jeremyschoffen.mbt.alpha.core.building.deps
  (:require
    [clojure.tools.deps.alpha.specs :as deps-specs]
    [clojure.tools.deps.alpha.reader :as deps-reader]
    [com.jeremyschoffen.mbt.alpha.core.specs]
    [com.jeremyschoffen.mbt.alpha.utils :as u]
    [clojure.spec.alpha :as s]))


(defn get-deps [{wd :project/working-dir}]
  (deps-reader/slurp-deps (u/safer-path wd "deps.edn")))

(u/spec-op get-deps
           :param {:req [:project/working-dir]}
           :ret :project/deps)


(defn make-symbolic-coord [{group-id   :maven/group-id
                            name       :maven/artefact-name
                            classifier :maven/classifier}]
  (symbol (str group-id) (str name (when classifier
                                     (str "$" classifier)))))

(u/spec-op make-symbolic-coord
           :param {:req [:maven/group-id
                         :maven/artefact-name]
                   :opt [:maven/classifier]}
           :ret ::deps-specs/lib)


(defn make-deps-coords [{v :project/version
                         :as param}]
  {(make-symbolic-coord param) {:mvn/version v}})

(u/spec-op make-deps-coords
           :deps [make-symbolic-coord]
           :param {:req [:maven/group-id
                         :maven/artefact-name
                         :project/version]
                   :opt [:maven/classifier]}
           :ret (s/map-of ::deps-specs/lib :mvn/coord))
