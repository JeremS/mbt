(ns ^{:author "Jeremy Schoffen"
      :doc "
Api used when versioning the project using git state.
      "}
  fr.jeremyschoffen.mbt.alpha.default.versioning.schemes
  (:require
    [fr.jeremyschoffen.mbt.alpha.core.specs]
    [fr.jeremyschoffen.mbt.alpha.default.specs]
    [fr.jeremyschoffen.mbt.alpha.default.versioning.schemes.maven-like :as maven-like]
    [fr.jeremyschoffen.mbt.alpha.default.versioning.schemes.git-distance :as git-distance]
    [fr.jeremyschoffen.mbt.alpha.default.versioning.schemes.protocols :as vp]
    [fr.jeremyschoffen.mbt.alpha.utils :as u]))

;;----------------------------------------------------------------------------------------------------------------------
;; Smoothing the polymorphic interface
;;----------------------------------------------------------------------------------------------------------------------
(defn initial-version
  "Get the initial version for a version scheme."
  [{h :versioning/scheme}]
  (vp/initial-version h))

(u/spec-op initial-version
           :param {:req [:versioning/scheme]}
           :ret :versioning/version)


(defn current-version
  "Get the current version using a version scheme."
  [{s :versioning/scheme
    desc :git/description}]
  (vp/current-version s desc))

(u/spec-op current-version
           :param {:req [:versioning/scheme :git/description]}
           :ret :versioning/version)


(defn bump
  "Bump a version using a version scheme."
  [{s :versioning/scheme
    v :versioning/version
    l :versioning/bump-level}]
  (if l
    (vp/bump s v l)
    (vp/bump s v)))

(u/spec-op bump
           :param {:req [:versioning/scheme :versioning/version]
                   :opt [:versioning/bump-level]}
           :ret :versioning/version)


;;----------------------------------------------------------------------------------------------------------------------
;; Default version schemes
;;----------------------------------------------------------------------------------------------------------------------
(u/def-clone maven-scheme maven-like/maven-scheme)
(u/def-clone semver-scheme maven-like/semver-scheme)
(u/def-clone git-distance-scheme git-distance/simple-scheme)
