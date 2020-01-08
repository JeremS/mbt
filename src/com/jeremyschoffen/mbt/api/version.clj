(ns com.jeremyschoffen.mbt.api.version
  (:require
    [clojure.spec.alpha :as s]
    [com.jeremyschoffen.mbt.api.specs]
    [com.jeremyschoffen.mbt.api.version.protocols :as vp]
    [com.jeremyschoffen.mbt.api.version.simple-version :as simple]
    [com.jeremyschoffen.mbt.api.version.metav.maven :as maven]
    [com.jeremyschoffen.mbt.api.version.metav.semver :as semver]))


(def simple-scheme simple/version-scheme)
(def maven-scheme maven/version-scheme)
(def semver-scheme semver/version-scheme)


(s/fdef vp/initial-version
        :args (s/cat :version-scheme :version/scheme))


(s/fdef vp/current-version
        :args (s/cat :version-scheme :version/scheme
                     :state (s/keys :req [:git/repo :artefact/name])))

(s/fdef vp/bump
        :args (s/cat :version-scheme :version/scheme
                     :state (s/keys :req [:git/repo :artefact/name])
                     :level (s/? :version/bump-level)))



