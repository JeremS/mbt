(ns ^{:author "Jeremy Schoffen"
      :doc "
Higher level apis.
      "}
  fr.jeremyschoffen.mbt.alpha.default.tasks
  (:require
    [clojure.spec.alpha :as s]
    [cognitect.anomalies :as anom]

    [fr.jeremyschoffen.mbt.alpha.core :as mbt-core]
    [fr.jeremyschoffen.mbt.alpha.default.jar :as default-jar]
    [fr.jeremyschoffen.mbt.alpha.default.versioning :as v]
    [fr.jeremyschoffen.mbt.alpha.default.specs]
    [fr.jeremyschoffen.mbt.alpha.utils :as u]))


(u/pseudo-nss
  build
  build.jar
  build.uberjar
  git
  git.commit
  jar
  jar.manifest
  maven
  maven.pom
  maven.scm
  project
  project.deps
  versioning)


;;----------------------------------------------------------------------------------------------------------------------
;; Pre bump generation
;;----------------------------------------------------------------------------------------------------------------------
(defn- commit-generated! [conf]
  (-> conf
      (update ::git/commit! u/ensure-v ::git.commit/message "Added generated files.")
      mbt-core/git-commit!))

(u/spec-op commit-generated!
           :deps [mbt-core/git-commit!]
           :param {:req [::git/repo
                         ::git/commit!]})


(defn generate-then-commit!
  "Helper function intended to be used to automatically commit some generated files.

  Several steps are taken:

  1) Check the repo using [[fr.jeremyschoffen.mbt.alpha.default.versioning/check-repo-in-order]].
  2) Thread `conf` through `fns` using [[fr.jeremyschoffen.mbt.alpha.utils//thread-fns]].
  3) Add all the new files to git using [[fr.jeremyschoffen.mbt.alpha.core/git-add-all!]]
  4) Commit all the generated files.


  Args:
  - `conf`: a map, the build's configuration
  - `fns`: functions, presumably functions generating docs or a version file."
  [conf & fns]
  (-> conf
      (u/check v/check-repo-in-order)
      (as-> conf (apply u/thread-fns conf fns))
      (u/do-side-effect! mbt-core/git-add-all!)
      (u/do-side-effect! commit-generated!)))

(s/fdef generate-then-commit!
        :args (s/cat :param (s/keys :req [::git/repo
                                          ::project/version
                                          ::versioning/scheme]
                                    :opt [::versioning/tag-base-name
                                          ::versioning/bump-level])
                     :fns (s/* fn?)))


;;----------------------------------------------------------------------------------------------------------------------
;; Building jars
;;----------------------------------------------------------------------------------------------------------------------
(defn check-incompatible-deps
  "Checks wheter or not the jar being built uses deps incompatible with maven (git libs, local deps)."
  [{allowed? ::build.jar/allow-non-maven-deps
    :as conf}]
  (let [non-maven-deps (mbt-core/deps-non-maven conf)]
    (when (and (seq non-maven-deps)
               (not allowed?))
      (throw (ex-info "Can't build a skinny jar while having non maven deps."
                      {::anom/category ::anom/forbidden
                       :mbt/error :invalid-deps
                       :faulty-deps non-maven-deps})))))

(defn jar!
  "Build a skinny jar for the project.
  Depending on the value of `:fr...mbt.alpha.build.jar/allow-non-maven-deps` this function will throw
  an exception if non maven deps are found.

  Also ensures several config keys are present using
  [[fr.jeremyschoffen.mbt.alpha.default.jar/ensure-jar-defaults]].
  "
  [param]
  (-> param
      (u/check check-incompatible-deps)
      default-jar/ensure-jar-defaults
      default-jar/jar!))

(u/spec-op jar!
           :deps [default-jar/ensure-jar-defaults default-jar/jar!]
           :param {:req [::build.jar/path
                         ::build.jar/allow-non-maven-deps
                         ::maven/artefact-name
                         ::maven/group-id
                         ::maven.pom/path
                         ::project/deps
                         ::project/version
                         ::project/working-dir]
                   :opt [::jar/exclude?
                         ::jar/main-ns
                         ::jar.manifest/overrides
                         ::maven/scm
                         ::project/author
                         ::project/licenses
                         ::project.deps/aliases]})


(defn uberjar!
  "Build an uberjar for the project. Ensure that the `:fr...mbt.alpha.project/version` is present int the config with
  [[fr.jeremyschoffen.mbt.alpha.default.versioning/current-project-version]].
  Also ensure other keys using [[fr.jeremyschoffen.mbt.alpha.default.building/ensure-jar-defaults]]."
  [param]
  (-> param
      default-jar/ensure-jar-defaults
      default-jar/uberjar!))

(u/spec-op uberjar!
           :deps [default-jar/ensure-jar-defaults
                  default-jar/uberjar!]
           :param {:req [::build.uberjar/path
                         ::maven/artefact-name
                         ::maven/group-id
                         ::maven.pom/path
                         ::project/deps
                         ::project/version
                         ::project/working-dir]
                   :opt [::jar/exclude?
                         ::jar/main-ns
                         ::jar.manifest/overrides
                         ::maven/scm
                         ::project/author
                         ::project/licenses
                         ::project.deps/aliases]})