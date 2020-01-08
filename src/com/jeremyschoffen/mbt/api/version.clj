(ns com.jeremyschoffen.mbt.api.version
  (:require
    [com.jeremyschoffen.mbt.api.version.simple-version :as simple]
    [com.jeremyschoffen.mbt.api.version.metav.maven :as maven]
    [com.jeremyschoffen.mbt.api.version.metav.semver :as semver]))


(def simple-scheme simple/version-scheme)
(def maven-scheme maven/version-scheme)
(def semver-scheme semver/version-scheme)
