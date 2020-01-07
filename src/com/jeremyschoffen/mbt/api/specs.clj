(ns com.jeremyschoffen.mbt.api.specs
  (:require
    [clojure.spec.alpha :as s]
    [com.jeremyschoffen.java.nio.file :as fs]
    [com.jeremyschoffen.mbt.api.version.protocols :as vp])
  (:import (org.eclipse.jgit.api Git)))

(def path? fs/path?)
(def path-like? (some-fn path? string?))

;;----------------------------------------------------------------------------------------------------------------------
;; General
;;----------------------------------------------------------------------------------------------------------------------
(s/def :project/working-dir (every-pred path-like? fs/absolute?))
(s/def :project/name string?)
(s/def :project/version any?)
(s/def :module/name string?)
(s/def :artefact/name string?)

(s/def :version/bump-level keyword?)
(s/def :version/scheme #(satisfies? vp/VersionScheme %))
;;----------------------------------------------------------------------------------------------------------------------
;; Git
;;----------------------------------------------------------------------------------------------------------------------
(s/def :git/repo #(isa? (type %) Git))
(s/def :git/top-level fs/path?)
(s/def :git/prefix (s/nilable (every-pred fs/path? (complement fs/absolute?))))

(s/def :git/basic-state (s/keys :req [:git/top-level :git/prefix :git/repo]))

(s/def :git.tag/name string?)
(s/def :git.tag/message string?)
(s/def :git/tag (s/keys :req [:git.tag/name :git.tag/message]))

(s/def :git.tag/sign? boolean?)

(s/def :git.describe/tag-pattern string?)
(s/def :git.describe/distance int?)
(s/def :git/sha string?)
(s/def :git.repo/dirty? boolean?)


(s/def :git/raw-description string?)
(s/def :git/description
  (s/keys :req [:git/raw-description
                :git/sha
                :git.describe/distance
                :git.tag/name
                :git.tag/message
                :git.repo/dirty?]))


