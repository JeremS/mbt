(ns ^{:author "Jeremy Schoffen"
      :doc "
Implementation of versioning schemes using the git disatnce building blocks from the core api
([[fr.jeremyschoffen.mbt.alpha.core.versioning.simple-version]]).
      "}
  fr.jeremyschoffen.mbt.alpha.default.versioning.schemes.git-distance
  (:require
    [fr.jeremyschoffen.mbt.alpha.core :as mbt-core]
    [fr.jeremyschoffen.mbt.alpha.default.specs]
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
                 mbt-core/version-parse-git-distance)
        relevant-part (-> git-desc
                          (select-keys #{:git/sha
                                         :git.describe/distance
                                         :git.repo/dirty?})
                          u/strip-keys-nss)]
    (mbt-core/version-git-distance (merge base relevant-part))))


(def git-distance-scheme
  "A simple version scheme based on git-distance. There is only one version number starting at 0 on a specific commit.
  It then becomes the number of commits from the initial version bumps after bumps.

  This system allows for the use of alpha and beta qualifiers. A version without a qualifier can't go back to alpha nor
  beta. To use them they need to be specified when the initial version is made conforming to the
  `:git-distance/qualifier` spec.

  To specify a qualifier:
  - using [[fr.jeremyschoffen.mbt.alpha.default.versioning.schemes.protocols/initial-version]] the `level` parameter is
  used
  - using [[fr.jeremyschoffen.mbt.alpha.default.versioning.schemes/initial-version]] the qualifier is passed using
  the key `:versioning/bump-level` of the config map.
  ``"
  (reify p/VersionScheme
    (current-version [_ desc]
      (current-version* desc))

    (initial-version [_]
      (mbt-core/version-initial-git-distance))

    (initial-version [_ level]
      (mbt-core/version-initial-git-distance level))

    (bump [_ version]
      (mbt-core/version-bump-git-distance version))

    (bump [_ version level]
      (mbt-core/version-bump-git-distance version level))))
