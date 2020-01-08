(ns com.jeremyschoffen.mbt.api.versioning.schemes.simple-version
  (:require
    [clojure.spec.alpha :as s]
    [cognitect.anomalies :as anom]
    [com.jeremyschoffen.mbt.api.git :as git]
    [com.jeremyschoffen.mbt.api.versioning.schemes.protocols :as vp]
    [com.jeremyschoffen.mbt.api.versioning.schemes.common :as common]
    [com.jeremyschoffen.mbt.api.utils :as u]))


(defrecord SimpleVersion [base-number distance sha dirty]
  Object
  (toString [_]
    (-> (str base-number)
        (cond-> (pos? distance) (str "." distance "-0x" sha)
                dirty           (str "-DIRTY")))))


(def initial-simple-version (SimpleVersion. 0 0 "" false))

;; TODO: get rid of the format, the regex shouldn't need the artefact's name
(defn- tag->version-number [artefact-name tag-name]
  (let [pattern (format "^%s-v(\\d*).*$" artefact-name)
        [_ n-str] (re-matches (re-pattern pattern) tag-name)]
    (Integer/parseInt n-str)))


(defn current-version* [{artefact-name :artefact/name
                         :as param}]
  (let [{tag-name :git.tag/name
         distance :git.describe/distance
         sha      :git/sha
         dirty    :git.repo/dirty?} (common/most-recent-description param)
        last-version-number (tag->version-number artefact-name tag-name)]
    (SimpleVersion. last-version-number distance sha dirty)))


(u/spec-op current-version*
           (s/keys :req [:git/repo :artefact/name])
           :project/version)

(defn- bump* [v]
  (let [{:keys [base-number distance sha dirty]} v]
    (when dirty
      (throw (ex-info (format "Can't bump a dirty version: %s" v)
                      {::anom/category ::anom/forbidden})))
    (SimpleVersion. (+ base-number distance) 0 sha dirty)))

(def version-scheme
  (reify vp/VersionScheme
    (initial-version [_]
      initial-simple-version)
    (current-version [_ state]
      (current-version* state))
    (bump [_ version]
      (bump* version))
    (bump [_ version _]
      (bump* version))))
