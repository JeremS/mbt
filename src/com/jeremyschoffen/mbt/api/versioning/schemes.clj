(ns com.jeremyschoffen.mbt.api.versioning.schemes
  (:require
    [com.jeremyschoffen.mbt.api.versioning.schemes.simple-version :as simple]
    [com.jeremyschoffen.mbt.api.versioning.schemes.metav.maven :as maven]
    [com.jeremyschoffen.mbt.api.versioning.schemes.metav.semver :as semver]))


(def simple-scheme simple/version-scheme)
(def maven-scheme maven/version-scheme)
(def semver-scheme semver/version-scheme)
