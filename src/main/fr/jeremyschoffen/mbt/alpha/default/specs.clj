(ns ^{:author "Jeremy Schoffen"
      :doc "
Specs used in the default api.
      "}
  fr.jeremyschoffen.mbt.alpha.default.specs
  (:require
    [clojure.spec.alpha :as s]
    [fr.jeremyschoffen.java.nio.alpha.file :as fs]

    [fr.jeremyschoffen.mbt.alpha.core.specs]
    [fr.jeremyschoffen.mbt.alpha.default.versioning.schemes.protocols :as vp]
    [fr.jeremyschoffen.mbt.alpha.utils :as u]))

(u/mbt-alpha-pseudo-nss
  project
  build
  build.jar
  maven.deploy
  versioning
  version-file)



(s/def ::project/name string?)
(s/def ::project/output-dir fs/path?)

;;----------------------------------------------------------------------------------------------------------------------
;; Jar building
;;----------------------------------------------------------------------------------------------------------------------
(s/def ::build.jar/output-dir fs/path?)

(defn- jar-ext? [s]
  (.endsWith (str s) ".jar"))

(s/def ::build/jar-name (every-pred string? jar-ext?))
(s/def ::build/uberjar-name (every-pred string? jar-ext?))


;;----------------------------------------------------------------------------------------------------------------------
;; Deployment
;;----------------------------------------------------------------------------------------------------------------------
(s/def ::maven.deploy/sign-artefacts? boolean?)


;;----------------------------------------------------------------------------------------------------------------------
;; Versioning
;;----------------------------------------------------------------------------------------------------------------------
(s/def ::versioning/bump-level keyword?)
(s/def ::versioning/scheme #(satisfies? vp/VersionScheme %))
(s/def ::versioning/tag-base-name string?)

(s/def ::versioning/version any?)

(s/def ::versioning/major keyword?)

(s/def ::version-file/path fs/path?)
(s/def ::version-file/ns symbol?)
