(ns ^{:author "Jeremy Schoffen"
      :doc "
Implementation of versioning schemes using the maven and semver building blocks from the core api
([[fr.jeremyschoffen.mbt.alpha.core.versioning.maven-like]]).
      "}
  fr.jeremyschoffen.mbt.alpha.default.versioning.schemes.maven-like
  (:require
    [fr.jeremyschoffen.mbt.alpha.core :as mbt-core]
    [fr.jeremyschoffen.mbt.alpha.default.specs]
    [fr.jeremyschoffen.mbt.alpha.default.versioning.schemes.protocols :as p]
    [fr.jeremyschoffen.mbt.alpha.utils :as u]))


(u/mbt-alpha-pseudo-nss
  git
  git.describe
  git.repo
  git.tag)


(defn- parse-git-descripton
  "Parse a git description into a map usable by a maven-like version cstr."
  [git-desc]
  (let [current-str (-> git-desc
                        (get-in  [::git/tag ::git.tag/message])
                        clojure.edn/read-string
                        :version)
        relevant-part (-> git-desc
                          (select-keys #{::git/sha
                                         ::git.describe/distance
                                         ::git.repo/dirty?})
                          u/strip-keys-nss)]
    (-> current-str
        mbt-core/version-parse-maven-like
        (merge relevant-part))))


(def maven-scheme
  "A maven version scheme. The versioning starts at the first versioned commit.
  Supports the following bumps:
  - `:patch`
  - `:minor`
  - `:major`
  - `:alpha`
  - `:beta`
  - `:rc`
  - `:release`
  "
  (reify p/VersionScheme
    (current-version [_ desc]
      (-> desc parse-git-descripton mbt-core/version-maven))

    (initial-version [_]
      mbt-core/version-initial-maven)

    (bump [this version]
      (p/bump this version :patch))

    (bump [_ version level]
      (mbt-core/version-bump-maven version level))))


(def semver-scheme
  "A semver version scheme. The versioning starts at the first versioned commit.
  Supports the following bumps:
  - `:patch`
  - `:minor`
  - `:major`
  "
  (reify p/VersionScheme
    (current-version [_ desc]
      (-> desc parse-git-descripton mbt-core/semver-version))

    (initial-version [_]
      mbt-core/version-initial-semver)

    (initial-version [_ _]
      mbt-core/version-initial-semver)

    (bump [this version]
      (p/bump this version :patch))

    (bump [_ version level]
      (mbt-core/version-bump-semver version level))))
