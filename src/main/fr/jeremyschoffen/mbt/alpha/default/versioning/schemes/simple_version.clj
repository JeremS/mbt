(ns ^{:author "Jeremy Schoffen"
      :doc "
Implementation of versioning schemes using the git disatnce building blocks from the core api
([[fr.jeremyschoffen.mbt.alpha.core.versioning.simple-version]]).
      "}
  fr.jeremyschoffen.mbt.alpha.default.versioning.schemes.simple-version
  (:require
    [fr.jeremyschoffen.mbt.alpha.core :as mbt-core]
    [fr.jeremyschoffen.mbt.alpha.default.versioning.schemes.protocols :as p]
    [fr.jeremyschoffen.mbt.alpha.utils :as u]))


(defn- current-version*
  "Parse a git description into the current version.`"
  [{tag :git/tag
    :as git-desc}]
  (let [base (-> tag
                 :git.tag/message
                 clojure.edn/read-string
                 :version
                 mbt-core/version-parse-simple)
        relevant-part (-> git-desc
                          (select-keys #{:git/sha
                                         :git.describe/distance
                                         :git.repo/dirty?})
                          u/strip-keys-nss)]
    (mbt-core/version-simple (merge base relevant-part))))


(def simple-scheme
  "A simple version scheme. There is only one version number. It's starts at 0 on a specific commit then is just the git
  distance from that initial versioned commit."
  (reify p/VersionScheme
    (current-version [_ desc]
      (current-version* desc))

    (initial-version [_]
      mbt-core/version-initial-simple)

    (bump [_ version]
      (mbt-core/version-simple-bump version))

    (bump [_ version level]
      (mbt-core/version-simple-bump version level))))
