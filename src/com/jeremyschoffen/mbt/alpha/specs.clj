(ns com.jeremyschoffen.mbt.alpha.specs
  (:require
    [clojure.spec.alpha :as s]
    [clojure.tools.deps.alpha.specs :as deps-specs]
    [com.jeremyschoffen.java.nio.file :as fs]
    [com.jeremyschoffen.mbt.alpha.versioning.schemes.protocols :as vp])
  (:import (org.eclipse.jgit.api Git)))

(def path? fs/path?)
(def path-like? (some-fn path? string?))

;;----------------------------------------------------------------------------------------------------------------------
;; General
;;----------------------------------------------------------------------------------------------------------------------
(s/def :project/working-dir (every-pred path-like? fs/absolute?))
(s/def :project/name string?)
(s/def :project/version any?)
(s/def :project/author string?)
(s/def :module/name string?)
(s/def :artefact/name string?)

;;----------------------------------------------------------------------------------------------------------------------
;; Versioning
;;----------------------------------------------------------------------------------------------------------------------
(s/def :version/bump-level keyword?)
(s/def :version/scheme #(satisfies? vp/VersionScheme %))

;;----------------------------------------------------------------------------------------------------------------------
;; Deps
;;----------------------------------------------------------------------------------------------------------------------
(s/def :project/deps ::deps-specs/deps-map)
(s/def :project.deps/aliases (s/coll-of keyword? :into #{}))

;;----------------------------------------------------------------------------------------------------------------------
;; Jar
;;----------------------------------------------------------------------------------------------------------------------
(s/def :jar/main-ns symbol?)
(s/def :jar.manifest/overrides map?)

;;----------------------------------------------------------------------------------------------------------------------
;; Compilation
;;----------------------------------------------------------------------------------------------------------------------
(s/def :clojure.compilation/namespaces (s/or :all  #{:all}
                                             :coll (s/coll-of symbol?)))
(s/def :clojure.compilation/include-external-sources boolean?)
(s/def :clojure.compilation/output-dir path?)
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

(def description-keys #{:git/raw-description
                        :git/sha
                        :git.describe/distance
                        :git.tag/name
                        :git.tag/message
                        :git.repo/dirty?})

