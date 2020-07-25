(ns com.jeremyschoffen.mbt.alpha.default.versioning.schemes
  (:require
    [com.jeremyschoffen.mbt.alpha.core.specs]
    [com.jeremyschoffen.mbt.alpha.default.versioning.schemes.maven-like :as maven-like]
    [com.jeremyschoffen.mbt.alpha.default.versioning.schemes.simple-version :as simple]
    [com.jeremyschoffen.mbt.alpha.default.versioning.schemes.protocols :as vp]
    [com.jeremyschoffen.mbt.alpha.default.specs]
    [com.jeremyschoffen.mbt.alpha.utils :as u]))

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
(u/alias-def maven-scheme maven-like/maven-scheme)
(u/alias-def semver-scheme maven-like/semver-scheme)
(u/alias-def simple-scheme simple/simple-scheme)
