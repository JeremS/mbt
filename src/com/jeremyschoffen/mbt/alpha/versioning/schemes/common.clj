(ns com.jeremyschoffen.mbt.alpha.versioning.schemes.common
  (:require
    [clojure.spec.alpha :as s]
    [com.jeremyschoffen.mbt.alpha.specs]
    [com.jeremyschoffen.mbt.alpha.utils :as u]
    [com.jeremyschoffen.mbt.alpha.git :as git]))


(defn most-recent-description [{repo :git/repo
                                artefact-name :artefact/name}]
  (git/describe {:git/repo repo
                 :git.describe/tag-pattern (str artefact-name "*")}))

(u/spec-op most-recent-description
           (s/keys :req [:git/repo :artefact/name]))