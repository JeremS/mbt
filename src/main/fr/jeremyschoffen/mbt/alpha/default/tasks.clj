(ns ^{:author "Jeremy Schoffen"
      :doc "
Higher level apis.
      "}
  fr.jeremyschoffen.mbt.alpha.default.tasks
  (:require
    [clojure.spec.alpha :as s]

    [fr.jeremyschoffen.mbt.alpha.core :as mbt-core]
    [fr.jeremyschoffen.mbt.alpha.default.jar :as jar]
    [fr.jeremyschoffen.mbt.alpha.default.versioning :as v]
    [fr.jeremyschoffen.mbt.alpha.default.specs]
    [fr.jeremyschoffen.mbt.alpha.utils :as u]))


(u/pseudo-nss
  build
  build.jar
  build.uberjar
  git
  git.commit
  jar.manifest
  maven
  maven.pom
  project
  project.deps
  versioning)


;;----------------------------------------------------------------------------------------------------------------------
;; Version file
;;----------------------------------------------------------------------------------------------------------------------
(defn anticipated-next-version
  "Get the next version of the project assuming the commit distance will be one more than it is now.

  Useful when the build process must generate and commit files before tagging the next version."
  [param]
  (let [current-version (v/current-version param)]
    (if-not current-version
      (v/schemes-initial-version param)
      (-> param
          (assoc ::versioning/version
                 (update current-version :distance inc))
          v/schemes-bump))))

(u/spec-op anticipated-next-version
           :deps [v/current-version v/schemes-initial-version v/schemes-bump]
           :param {:req [::git/repo
                         ::versioning/scheme]
                   :opt [::versioning/bump-level
                         ::versioning/tag-base-name]}
           :ret ::versioning/version)


(defn- commit-generated! [conf]
  (mbt-core/git-commit! (assoc conf
                          ::git/commit! {::git.commit/message "Added generated files."})))

(u/spec-op commit-generated!
           :deps [mbt-core/git-commit!]
           :param {:req [::git/repo
                         ::git/commit!]})


(defn generate-before-bump!
  "Helper function intended to be used just before tagging a new version. The idea here is that when we want to release
  a new version, we want to generate some docs or a version file for instance. These files will need to be generated,
  added and committed to the repo. Also, adding this commit may influence the version number of the realease.

  This function attemps to provide a way to encapsulate this logic. It is performed in several steps:

  1) Checks the repo using [[fr.jeremyschoffen.mbt.alpha.default.versioning/check-repo-in-order]].
  2) Ensure the the presence of the `:fr...mbt.alpha.project/version` key of the `conf` using
     [[fr.jeremyschoffen.mbt.alpha.default.tasks/anticipated-next-version]] if necessary.
  3) Thread `conf` through `fns` using [[fr.jeremyschoffen.mbt.alpha.utils//thread-fns]].
  4) Add all the new files to git using [[fr.jeremyschoffen.mbt.alpha.core/git-add-all!]]
  5) Commit all the generated files.


  Args:
  - `conf`: a map, the build's configuration
  - `fns`: functions, presumably functions generating docs or a version file."
  [conf & fns]
  (-> conf
      (u/check v/check-repo-in-order)
      (u/ensure-computed ::project/version (comp str anticipated-next-version))
      (as-> conf (apply u/thread-fns conf fns))
      (u/do-side-effect! mbt-core/git-add-all!)
      (u/do-side-effect! commit-generated!)))

(s/fdef generate-before-bump!
        :args (s/cat :param (s/keys :req [::git/repo
                                          ::versioning/scheme]
                                    :opt [::versioning/tag-base-name
                                          ::versioning/bump-level])
                     :fns (s/* fn?)))


;;----------------------------------------------------------------------------------------------------------------------
;; Building jars
;;----------------------------------------------------------------------------------------------------------------------
(defn jar!
  "Build a skinny jar for the project. Ensure that the `:fr...mbt.alpha.project/version` is present int the config with
  [[fr.jeremyschoffen.mbt.alpha.default.versioning/current-project-version]].
  Also ensure other keys using [[fr.jeremyschoffen.mbt.alpha.default.building/ensure-jar-defaults]].
  "
  [param]
  (-> param
      jar/ensure-jar-defaults
      jar/jar!))

(u/spec-op jar!
           :deps [jar/ensure-jar-defaults jar/jar!]
           :param {:req [::build.jar/path
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
      jar/ensure-jar-defaults
      jar/uberjar!))

(u/spec-op uberjar!
           :deps [jar/ensure-jar-defaults jar/uberjar!]
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