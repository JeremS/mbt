(ns com.jeremyschoffen.mbt.alpha.core.versioning.simple-version
  (:require
    [cognitect.anomalies :as anom]
    [com.jeremyschoffen.mbt.alpha.core.utils :as u]))


(defrecord SimpleVersion [base-number distance sha dirty]
  Object
  (toString [_]
    (-> (str base-number)
        (cond-> (pos? distance) (str "." distance "-0x" sha)
                dirty           (str "-DIRTY")))))


(def initial-simple-version (SimpleVersion. 0 0 "" false))


(def tag-pattern #".*-v(\d*).*$")


(defn- tag->version-number [tag-name]
  (let [[_ n-str] (re-matches tag-pattern tag-name)]
    (Integer/parseInt n-str)))


(defn- current-version* [{tag-name :git.tag/name
                          distance :git.describe/distance
                          sha      :git/sha
                          dirty    :git.repo/dirty?}]
  (let [last-version-number (tag->version-number tag-name)]
    (SimpleVersion. last-version-number distance sha dirty)))


(u/simple-fdef current-version*
               :git/description
               :project/version)


(defn- bump* [v]
  (let [{:keys [base-number distance sha dirty]} v]
    (when dirty
      (throw (ex-info (format "Can't bump a dirty version: %s" v)
                      {::anom/category ::anom/forbidden})))
    (SimpleVersion. (+ base-number distance) 0 sha dirty)))
