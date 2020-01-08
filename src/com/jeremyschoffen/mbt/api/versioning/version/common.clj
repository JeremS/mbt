(ns com.jeremyschoffen.mbt.api.versioning.version.common
  (:require
    [clojure.spec.alpha :as s]
    [com.jeremyschoffen.mbt.api.specs]
    [com.jeremyschoffen.mbt.api.utils :as u]
    [com.jeremyschoffen.mbt.api.git :as git]))


(defn most-recent-description [{repo :git/repo
                                artefact-name :artefact/name}]
  (git/describe {:git/repo repo
                 :git.describe/tag-pattern (str artefact-name "*")}))

(u/spec-op most-recent-description
           (s/keys :req [:git/repo :artefact/name]))