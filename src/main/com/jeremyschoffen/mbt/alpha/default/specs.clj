(ns com.jeremyschoffen.mbt.alpha.default.specs
  (:require
    [clojure.spec.alpha :as s]
    [com.jeremyschoffen.java.nio.alpha.file :as fs]
    [com.jeremyschoffen.mbt.alpha.core.specs]
    [com.jeremyschoffen.mbt.alpha.default.versioning.schemes.protocols :as vp]))

(s/def :project/name string?)
(s/def :project/output-dir fs/path?)

;;----------------------------------------------------------------------------------------------------------------------
;; Jar building
;;----------------------------------------------------------------------------------------------------------------------
(defn- jar-ext? [s]
  (.endsWith (str s) ".jar"))

(s/def :build/jar-name (every-pred string? jar-ext?))
(s/def :build/uberjar-name (every-pred string? jar-ext?))


;;----------------------------------------------------------------------------------------------------------------------
;; Deployment
;;----------------------------------------------------------------------------------------------------------------------
(s/def :maven.deploy/sign-artefacts? boolean?)


;;----------------------------------------------------------------------------------------------------------------------
;; Versioning
;;----------------------------------------------------------------------------------------------------------------------
(s/def :versioning/bump-level keyword?)
(s/def :versioning/scheme #(satisfies? vp/VersionScheme %))
(s/def :versioning/tag-base-name string?)

(s/def :versioning/version any?)

(s/def :versioning/major keyword?)

(s/def :version-file/path fs/path?)
(s/def :version-file/ns symbol?)

