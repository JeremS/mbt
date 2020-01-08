;; Taken from https://github.com/jgrodziski/metav
(ns com.jeremyschoffen.mbt.api.versioning.version.metav.common
  (:require
    [cognitect.anomalies :as anom]))

(defprotocol SCMHosted
  (subversions [this])
  (tag [this])
  (distance [this])
  (sha [this])
  (dirty? [this]))


(defprotocol Bumpable
  (bump* [this level]))


(def default-initial-subversions [0 1 0])


(defn bump-subversions [subversions level]
  (let [[major minor patch] subversions]
    (case level
      :major [(inc major) 0 0]
      :minor [major (inc minor) 0]
      :patch [major minor (inc patch)])))


(defn duplicating-version? [v level]
  (let [[_ minor patch] (subversions v)
        distance (distance v)
        same-patch? (= level :patch)
        same-minor? (and (= level :minor)
                         (= patch 0))
        same-major? (and (= level :major)
                         (= patch 0)
                         (= minor 0))]
    (and (= distance 0)
         (or same-patch?
             same-minor?
             same-major?))))


(defn going-backwards? [old-version new-version]
  (pos? (compare old-version new-version)))


(defn assert-bump? [old-version level new-version]
  (when (duplicating-version? old-version level)
    (throw (ex-info (str "Aborted released, bumping with level: " level
                         " would create version: " new-version " pointing to the same commit as version: " old-version ".")
                    {::anom/category ::anom/forbidden
                     :mbt/error :versioning/duplicating-tag})))
  (when (going-backwards? old-version new-version)
    (throw (ex-info (str "Can't bump version to an older one : " old-version " -> " new-version " isn't allowed.")
                    {::anom/category ::anom/forbidden
                     :mbt/error :versioning/going-backward}))))

(defn safer-bump [v level]
  (let [new-v (bump* v level)]
    (assert-bump? v level new-v)
    new-v))
