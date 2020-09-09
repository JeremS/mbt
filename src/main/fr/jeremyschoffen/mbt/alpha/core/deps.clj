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


(defn get-all-deps
  "Get the system-wide, user wide and project wide deps merged together
  The deps file's path of the project's deps is passed under the key `:...mbt.alpha.project.deps/file`."
  [{p ::project.deps/file}]
  (let [{:keys [root-edn user-edn project-edn]} (deps/find-edn-maps p)]
    (deps/merge-edns (filterv some? [root-edn user-edn project-edn]))))

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


(defn make-symbolic-name
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

(u/spec-op make-symbolic-name
           :param {:req [::maven/group-id
                         ::maven/artefact-name]
                   :opt [::maven/classifier]}
           :ret ::deps-specs/lib)
