(ns com.jeremyschoffen.mbt.alpha.default.versioning.schemes.simple-version
  (:require
    [com.jeremyschoffen.mbt.alpha.core :as mbt-core]
    [com.jeremyschoffen.mbt.alpha.core.utils :as u]
    [com.jeremyschoffen.mbt.alpha.default.versioning.schemes.protocols :as p]))


(defn- current-version* [{tag :git/tag
                          distance :git.describe/distance
                          sha      :git/sha
                          dirty    :git.repo/dirty?
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
    (mbt-core/simple-version (assoc relevant-part :number last-version-number))))


(def simple-scheme
  (reify p/VersionScheme
    (current-version [_ desc]
      (current-version* desc))

    (initial-version [_]
      mbt-core/initial-simple-version)

    (bump [_ version]
      (mbt-core/simple-version-bump version))

    (bump [_ version _]
      (mbt-core/simple-version-bump version))))
