(ns com.jeremyschoffen.mbt.alpha.default.versioning.schemes.maven-like
  (:require
    [com.jeremyschoffen.mbt.alpha.core :as mbt-core]
    [com.jeremyschoffen.mbt.alpha.core.utils :as u]
    [com.jeremyschoffen.mbt.alpha.default.versioning.schemes.protocols :as p]))

(defn parse-git-descripton [git-desc]
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
        mbt-core/parse-maven-like-version
        (merge relevant-part))))


(def maven-scheme
  (reify p/VersionScheme
    (current-version [_ desc]
      (-> desc parse-git-descripton mbt-core/maven-version))

    (initial-version [_]
      mbt-core/initial-maven-version)

    (bump [this version]
      (p/bump this version :patch))

    (bump [_ version level]
      (mbt-core/maven-bump version level))))


(def semver-scheme
  (reify p/VersionScheme
    (current-version [_ desc]
      (-> desc parse-git-descripton mbt-core/semver-version))

    (initial-version [_]
      mbt-core/initial-semver-version)

    (bump [this version]
      (p/bump this version :patch))

    (bump [_ version level]
      (mbt-core/semver-bump version level))))
