(ns com.jeremyschoffen.mbt.alpha.default
  (:require
    [com.jeremyschoffen.mbt.alpha.default.building :as building]
    [com.jeremyschoffen.mbt.alpha.default.defaults :as defaults]
    [com.jeremyschoffen.mbt.alpha.default.versioning :as versioning]
    [com.jeremyschoffen.mbt.alpha.utils :as u]))


(u/alias-fn make-conf defaults/make-context)

(def maven-scheme versioning/maven-scheme)
(def semver-scheme versioning/semver-scheme)
(def simple-scheme versioning/simple-scheme)

(u/alias-fn current-version versioning/current-version)
(u/alias-fn initial-version versioning/schemes-initial-version)
(u/alias-fn bump-version versioning/schemes-bump)
(u/alias-fn next-version versioning/next-version)
(u/alias-fn check-repo-in-order versioning/check-repo-in-order)
(u/alias-fn bump-tag! versioning/bump-tag!)

(u/alias-fn make-version-file versioning/make-version-file)
(u/alias-fn write-version-file! versioning/write-version-file!)

(u/alias-fn ensure-jar-defaults building/ensure-jar-defaults)
(u/alias-fn jar! building/jar!)
(u/alias-fn uberjar! building/uberjar!)