(ns ^{:author "Jeremy Schoffen"
      :doc"
Grouping of the different versioning utilities.
      "}
  fr.jeremyschoffen.mbt.alpha.default.versioning
  (:require
    [fr.jeremyschoffen.mbt.alpha.core.specs]
    [fr.jeremyschoffen.mbt.alpha.default.versioning.git-state :as git-state]
    [fr.jeremyschoffen.mbt.alpha.default.versioning.schemes :as schemes]
    [fr.jeremyschoffen.mbt.alpha.default.versioning.version-file :as vf]
    [fr.jeremyschoffen.mbt.alpha.default.specs]
    [fr.jeremyschoffen.mbt.alpha.utils :as u]))


(u/pseudo-nss
  git
  versioning)


(u/def-clone maven-scheme schemes/maven-scheme)
(u/def-clone semver-scheme schemes/semver-scheme)
(u/def-clone git-distance-scheme schemes/git-distance-scheme)

(u/def-clone schemes-current-version schemes/current-version)
(u/def-clone schemes-initial-version schemes/initial-version)
(u/def-clone schemes-bump schemes/bump)

(u/def-clone most-recent-description git-state/most-recent-description)
(u/def-clone current-version git-state/current-version)
(u/def-clone next-version git-state/next-version)
(u/def-clone next-tag git-state/next-tag)
(u/def-clone tag! git-state/tag!)
(u/def-clone check-repo-in-order git-state/check-repo-in-order)
(u/def-clone bump-tag! git-state/bump-tag!)

(u/def-clone write-version-file! vf/write-version-file!)


(defn current-project-version
  "Get the project's current version using git state and the provided version scheme then
  get its string representation."
  [param]
  (-> param
      current-version
      str))

(u/spec-op current-project-version
           :deps [current-version]
           :param {:req [::git/repo
                         ::versioning/scheme]
                   :opt [::versioning/tag-base-name]}
           :ret string?)