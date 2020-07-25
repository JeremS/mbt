(ns com.jeremyschoffen.mbt.alpha.core.building.deps
  (:require
    [clojure.tools.deps.alpha.specs :as deps-specs]
    [clojure.tools.deps.alpha.reader :as deps-reader]
    [com.jeremyschoffen.mbt.alpha.core.specs]
    [com.jeremyschoffen.mbt.alpha.utils :as u]
    [clojure.spec.alpha :as s]))


(defn get-deps
  "Slurp the deps.edn file of a project using [[clojure.tools.deps.alpha.reader/slurp-deps]]. The `deps.edn` file is
  expected to be found directly under the working directory specified under the key `:project/working-dir`."
  [{wd :project/working-dir}]
  (deps-reader/slurp-deps (u/safer-path wd "deps.edn")))

(u/spec-op get-deps
           :param {:req [:project/working-dir]}
           :ret :project/deps)


(defn make-symbolic-coord
  "Create a Clojure symbol following the clojure tools deps format used to specified maven dependencies.

  For instance the hypothetical:
  ```clojure
  (make-symbolic-coord
    {:maven/group-id 'org.clojure
     :maven/artefact-name 'clojure
     :maven/classifier 'docs})
  ;=> org.clojure/clojure$docs
  ```
  "
  [{group-id   :maven/group-id
    name       :maven/artefact-name
    classifier :maven/classifier}]
  (symbol (str group-id) (str name (when classifier
                                     (str "$" classifier)))))

(u/spec-op make-symbolic-coord
           :param {:req [:maven/group-id
                         :maven/artefact-name]
                   :opt [:maven/classifier]}
           :ret ::deps-specs/lib)


(defn make-deps-coords
  "Make the map representation of a dependency in a `deps.edn` file.

  For instance:
  ```clojure
  (make-deps-coord
    {:maven/group-id 'org.clojure
     :maven/artefact-name 'clojure
     :project/version \"10.0.1\"})
  ;=> {:org.clojure/clojure {:mvn/version \"10.0.1\"}}
  ```
  "
  [{v :project/version
    :as param}]
  {(make-symbolic-coord param) {:mvn/version v}})

(u/spec-op make-deps-coords
           :deps [make-symbolic-coord]
           :param {:req [:maven/group-id
                         :maven/artefact-name
                         :project/version]
                   :opt [:maven/classifier]}
           :ret (s/map-of ::deps-specs/lib :mvn/coord))
