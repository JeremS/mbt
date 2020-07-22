(ns com.jeremyschoffen.mbt.alpha.default.versioning.schemes.maven-like
  (:require
    [com.jeremyschoffen.mbt.alpha.core :as mbt-core]
    [com.jeremyschoffen.mbt.alpha.default.versioning.schemes.protocols :as p]
    [com.jeremyschoffen.mbt.alpha.utils :as u]))


(defn- parse-git-descripton
  "Parse a git description into a map usable by a maven-like version cstr."
  [git-desc]
  (let [current-str (-> git-desc
                        (get-in  [:git/tag :git.tag/message])
                        clojure.edn/read-string
                        :version)
        relevant-part (-> git-desc
                          (select-keys #{:git/sha
                                         :git.describe/distance
                                         :git.repo/dirty?})
                          u/strip-keys-nss)]
    (-> current-str
        mbt-core/version-parse-maven-like
        (merge relevant-part))))


(def maven-scheme
  "A maven version scheme."
  (reify p/VersionScheme
    (current-version [_ desc]
      (-> desc parse-git-descripton mbt-core/version-maven))

    (initial-version [_]
      mbt-core/version-initial-maven)

    (bump [this version]
      (p/bump this version :patch))

    (bump [_ version level]
      (mbt-core/version-maven-bump version level))))


(def semver-scheme
  "A Semver version scheme."
  (reify p/VersionScheme
    (current-version [_ desc]
      (-> desc parse-git-descripton mbt-core/semver-version))

    (initial-version [_]
      mbt-core/version-initial-semver)

    (bump [this version]
      (p/bump this version :patch))

    (bump [_ version level]
      (mbt-core/version-semver-bump version level))))
