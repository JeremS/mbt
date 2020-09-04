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


(u/def-clone most-recent-description git-state/most-recent-description)
(u/def-clone current-version git-state/current-version)
(u/def-clone next-version git-state/next-version)

(u/def-clone tag! git-state/tag!)
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


(defn make-next-version+x
  "Make a `next-version` function that adds `x` to the git distance of the version
  returned.

  Useful when anticipating the number of commits before tagging a release."
  [x]
  (fn [conf]
    (let [next-v (next-version conf)
          initial (schemes/initial-version conf)]
      (-> next-v
          (cond-> (not= initial next-v)
                  (update :distance + x))))))


(def next-version+1
  "Compute the next version with an increased git distance to take into account the
  commit created by the docs / versionfile generation."
  (make-next-version+x 1))

(u/spec-op next-version+1
           :deps [next-version]
           :param {:req [::git/repo
                         ::versioning/scheme]
                   :opt [::versioning/bump-level
                         ::versioning/tag-base-name]})


(defn project-version
  "Get a `...mbt.alpha.project/version` from a `...mbt.alpha.versioning/version`."
  [{v ::versioning/version}]
  (str v))

(u/spec-op project-version
           :param {:opt [::versioning/version]}
           :ret ::project/version)
