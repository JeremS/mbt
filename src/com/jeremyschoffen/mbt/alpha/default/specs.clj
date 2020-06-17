(ns com.jeremyschoffen.mbt.alpha.default.specs
  (:require
    [clojure.spec.alpha :as s]
    [com.jeremyschoffen.mbt.alpha.default.versioning.schemes.protocols :as vp]))


(def tag-name-regex #"(.*)-v(\d.*)")
;;----------------------------------------------------------------------------------------------------------------------
;; Versioning
;;----------------------------------------------------------------------------------------------------------------------
(s/def :versioning/bump-level keyword?)
(s/def :versioning/scheme #(satisfies? vp/VersionScheme %))
(s/def :versioning/tag-base-name string?)
(s/def :versioning/tag-name #(re-matches tag-name-regex %))
(s/def :versioning/version any?)



(s/def :version/bump-level (constantly false))
(s/def :version/scheme (constantly false))