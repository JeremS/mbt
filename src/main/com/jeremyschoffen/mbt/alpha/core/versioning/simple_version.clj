(ns com.jeremyschoffen.mbt.alpha.core.versioning.simple-version
  (:require
    [cognitect.anomalies :as anom]
    [com.jeremyschoffen.mbt.alpha.core.specs]
    [com.jeremyschoffen.mbt.alpha.utils :as u]))


(defrecord SimpleVersion [number distance sha dirty?]
  Object
  (toString [_]
    (-> (str number)
        (cond-> (pos? distance) (str "-" distance "-g" sha)
                dirty? (str "-DIRTY")))))


(def initial-simple-version
  "Initial value to use when starting the versioning process from scratch."
  (SimpleVersion. 0 0 "" false))


(defn parse-version-number [s]
  (Integer/parseInt s))

(u/simple-fdef parse-version-number
               string?
               :simple-version/number)


(defn simple-version
  "Make a representation of a version in a simple style.
  Here the version is ultimately just 1 number counting the number of commits since version 0."
  [x]
  (map->SimpleVersion x))

(u/spec-op simple-version
           :param {:req-un [:simple-version/number
                            :git.describe/distance
                            :git/sha
                            :git.repo/dirty?]})


(defn bump [v]
  (let [{:keys [number distance sha dirty?]} v]
    (when (zero? distance)
      (throw (ex-info "Duplicating tag."
                      {::anom/category ::anom/forbidden
                       :mbt/error :versioning/duplicating-tag})))
    (SimpleVersion. (+ number distance) 0 sha dirty?)))

