(ns com.jeremyschoffen.mbt.alpha.versioning.schemes
  (:require
    [com.jeremyschoffen.mbt.alpha.specs]
    [com.jeremyschoffen.mbt.alpha.versioning.schemes.protocols :as vp]
    [com.jeremyschoffen.mbt.alpha.versioning.schemes.simple-version :as simple]
    [com.jeremyschoffen.mbt.alpha.versioning.schemes.metav.maven :as maven]
    [com.jeremyschoffen.mbt.alpha.versioning.schemes.metav.semver :as semver]
    [com.jeremyschoffen.mbt.alpha.utils :as u]))


(def simple-scheme simple/version-scheme)
(def maven-scheme maven/version-scheme)
(def semver-scheme semver/version-scheme)


(defn initial-version [{h :version/scheme}]
  (vp/initial-version h))

(u/spec-op initial-version
           :param {:req [:version/scheme]})


(defn current-version [{s :version/scheme
                        desc :git/description}]
  (vp/current-version s desc))

(u/spec-op current-version
           :param {:req [:version/scheme :git/description]})


(defn bump [{s :version/scheme
             v :project/version
             l :version/bump-level}]
  (if l
    (vp/bump s v l)
    (vp/bump s v)))

(u/spec-op bump
           :param {:req [:version/scheme :project/version]
                   :opt [:version/bump-level]})
