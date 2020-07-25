(ns com.jeremyschoffen.mbt.alpha.default.versioning.schemes.simple-version
  (:require
    [com.jeremyschoffen.mbt.alpha.core :as mbt-core]
    [com.jeremyschoffen.mbt.alpha.default.versioning.schemes.protocols :as p]
    [com.jeremyschoffen.mbt.alpha.utils :as u]))


(defn- current-version*
  "Parse a git description into the current version.`"
  [{tag :git/tag
    :as git-desc}]
  (let [last-version-number (-> tag
                                :git.tag/message
                                clojure.edn/read-string
                                :version
                                Integer/parseInt)
        relevant-part (-> git-desc
                          (select-keys #{:git/sha
                                         :git.describe/distance
                                         :git.repo/dirty?})
                          u/strip-keys-nss)]
    (mbt-core/version-simple (assoc relevant-part :number last-version-number))))


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

    (bump [_ version _]
      (mbt-core/version-simple-bump version))))
