(ns com.jeremyschoffen.mbt.alpha.default.versioning
  (:require
    [com.jeremyschoffen.mbt.alpha.core.specs]
    [com.jeremyschoffen.mbt.alpha.default.versioning.git-state :as git-state]
    [com.jeremyschoffen.mbt.alpha.default.versioning.schemes :as schemes]
    [com.jeremyschoffen.mbt.alpha.default.versioning.version-file :as vf]
    [com.jeremyschoffen.mbt.alpha.default.specs]
    [com.jeremyschoffen.mbt.alpha.utils :as u]))


(def maven-scheme schemes/maven-scheme)
(def semver-scheme schemes/semver-scheme)
(def simple-scheme schemes/simple-scheme)

(u/alias-fn  schemes-current-version schemes/current-version)
(u/alias-fn schemes-initial-version schemes/initial-version)
(u/alias-fn schemes-bump schemes/bump)

(u/alias-fn most-recent-description git-state/most-recent-description)
(u/alias-fn current-version git-state/current-version)
(u/alias-fn next-version git-state/next-version)
(u/alias-fn next-tag git-state/next-tag)
(u/alias-fn tag! git-state/tag!)
(u/alias-fn check-repo-in-order git-state/check-repo-in-order)
(u/alias-fn bump-tag! git-state/bump-tag!)

(u/alias-fn make-version-file vf/make-version-file)
(u/alias-fn write-version-file! vf/write-version-file!)

