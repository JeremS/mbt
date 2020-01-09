(ns com.jeremyschoffen.mbt.alpha.versioning.schemes
  (:require
    [com.jeremyschoffen.mbt.alpha.versioning.schemes.simple-version :as simple]
    [com.jeremyschoffen.mbt.alpha.versioning.schemes.metav.maven :as maven]
    [com.jeremyschoffen.mbt.alpha.versioning.schemes.metav.semver :as semver]))


(def simple-scheme simple/version-scheme)
(def maven-scheme maven/version-scheme)
(def semver-scheme semver/version-scheme)
