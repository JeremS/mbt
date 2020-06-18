(ns com.jeremyschoffen.mbt.alpha.default.versioning.schemes.maven-like
  (:require
    [com.jeremyschoffen.mbt.alpha.core.versioning.maven-like :as m]
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
        m/parse-version
        (merge relevant-part))))


(def maven-scheme
  (reify p/VersionScheme
    (current-version [_ desc]
      (-> desc parse-git-descripton m/maven-version))

    (initial-version [_]
      (m/maven-version))

    (bump [this desc]
      (p/bump this desc :patch))

    (bump [_ version level]
      (m/safer-bump version level))))


(def semver-scheme
  (reify p/VersionScheme
    (current-version [_ desc]
      (-> desc parse-git-descripton m/semver-version))

    (initial-version [_]
      (m/semver-version))

    (bump [this desc]
      (p/bump this desc :patch))

    (bump [_ version level]
      (m/safer-bump version level))))
