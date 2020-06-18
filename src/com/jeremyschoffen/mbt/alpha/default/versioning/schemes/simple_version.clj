(ns com.jeremyschoffen.mbt.alpha.default.versioning.schemes.simple-version
  (:require
    [com.jeremyschoffen.mbt.alpha.core.versioning.simple-version :as sv]
    [com.jeremyschoffen.mbt.alpha.core.utils :as u]
    [com.jeremyschoffen.mbt.alpha.default.versioning.schemes.protocols :as p]))


(defn- current-version* [{tag :git/tag
                          distance :git.describe/distance
                          sha      :git/sha
                          dirty    :git.repo/dirty?}]
  (let [last-version-number (-> tag
                                :git.tag/message
                                clojure.edn/read-string
                                :version
                                Integer/parseInt)]
    (sv/->SimpleVersion last-version-number distance sha dirty)))


(def simple-scheme
  (reify p/VersionScheme
    (current-version [_ desc]
      (current-version* desc))

    (initial-version [_]
      sv/initial-simple-version)

    (bump [_ version]
      (sv/bump version))

    (bump [_ version _]
      (sv/bump version))))
