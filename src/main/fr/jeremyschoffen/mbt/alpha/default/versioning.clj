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
  project
  versioning)


(u/def-clone maven-scheme schemes/maven-scheme)
(u/def-clone semver-scheme schemes/semver-scheme)
(u/def-clone git-distance-scheme schemes/git-distance-scheme)


(u/def-clone schemes-initial-version schemes/initial-version)
(u/def-clone current-version git-state/current-version)
(u/def-clone next-version git-state/next-version)

(u/def-clone tag-name git-state/tag-name)
(u/def-clone check-repo-in-order git-state/check-repo-in-order)
(u/def-clone tag-new-version! git-state/tag-new-version!)

(u/def-clone write-version-file! vf/write-version-file!)


(defn last-version
  "Return the last tagged version."
  [param]
  (-> param
      current-version
      (assoc :distance 0)))

(u/spec-op last-version
           :deps [current-version]
           :param {:req [::git/repo
                         ::versioning/scheme]
                   :opt [:versioning/tag-base-name]}
           :ret ::versioning/version)


(defn project-version
  "Get a `...mbt.alpha.project/version` from a `...mbt.alpha.versioning/version`."
  [{v ::versioning/version}]
  (str v))

(u/spec-op project-version
           :param {:opt [::versioning/version]}
           :ret ::project/version)
