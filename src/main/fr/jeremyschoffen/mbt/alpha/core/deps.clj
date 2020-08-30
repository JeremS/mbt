(ns ^{:author "Jeremy Schoffen"
      :doc "
Api providing some utilities working with `clojure.tools.deps`.
      "}
  fr.jeremyschoffen.mbt.alpha.core.deps
  (:require
    [clojure.tools.deps.alpha.specs :as deps-specs]
    [clojure.tools.deps.alpha :as deps]
    [fr.jeremyschoffen.java.nio.alpha.file :as fs]
    [fr.jeremyschoffen.mbt.alpha.core.specs]
    [fr.jeremyschoffen.mbt.alpha.utils :as u]
    [clojure.spec.alpha :as s]))

(u/pseudo-nss
  maven
  project
  project.deps)


(defn get-deps
  "Slurp the deps.edn file of a project using [[clojure.tools.deps.alpha.reader/slurp-deps]].
  The deps file's path is passed under the key `:fr...mbt.alpha.project.deps/file`."
  [{p ::project.deps/file}]
  (-> p
      fs/file
      deps/slurp-deps))

(u/spec-op get-deps
           :param {:req [::project.deps/file]}
           :ret ::project/deps)


(defn non-maven-deps
  "Utility signaling non maven deps. These deps can't go into a pom file."
  [{deps-map ::project/deps}]
  (into #{}
        (keep (fn [[k v]]
                (when-not (contains? v :mvn/version)
                  k)))
        (:deps deps-map)))

(u/spec-op non-maven-deps
           :param {:req [::project/deps]}
           :ret (s/coll-of symbol? :kind set?))


(defn make-symbolic-coord
  "Create a Clojure symbol following the clojure tools deps format used to specified maven dependencies.

  For instance the hypothetical:
  ```clojure
  (make-symbolic-coord
    {::fr...mbt.alpha.maven/group-id 'org.clojure
     ::fr...mbt.alpha.maven/artefact-name 'clojure
     ::fr...mbt.alpha.maven/classifier 'docs})
  ;=> org.clojure/clojure$docs
  ```
  "
  [{group-id   ::maven/group-id
    name       ::maven/artefact-name
    classifier ::maven/classifier}]
  (symbol (str group-id) (str name (when classifier
                                     (str "$" classifier)))))

(u/spec-op make-symbolic-coord
           :param {:req [::maven/group-id
                         ::maven/artefact-name]
                   :opt [::maven/classifier]}
           :ret ::deps-specs/lib)


(defn make-deps-coords
  "Make the map representation of a dependency in a `deps.edn` file.

  For instance:
  ```clojure
  (make-deps-coord
    {::fr...mbt.alpha.maven/group-id 'org.clojure
     ::fr...mbt.alpha.maven/artefact-name 'clojure
     ::fr...mbt.alpha.project/version \"10.0.1\"})
  ;=> {:org.clojure/clojure {:mvn/version \"10.0.1\"}}
  ```
  "
  [{v ::project/version
    :as param}]
  {(make-symbolic-coord param) {:mvn/version v}})

(u/spec-op make-deps-coords
           :deps [make-symbolic-coord]
           :param {:req [::maven/group-id
                         ::maven/artefact-name
                         ::project/version]
                   :opt [::maven/classifier]}
           :ret (s/map-of ::deps-specs/lib :mvn/coord))
