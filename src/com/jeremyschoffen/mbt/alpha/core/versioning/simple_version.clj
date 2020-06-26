(ns com.jeremyschoffen.mbt.alpha.core.versioning.simple-version
  (:require
    [cognitect.anomalies :as anom]
    [com.jeremyschoffen.mbt.alpha.core.specs :as specs]
    [com.jeremyschoffen.mbt.alpha.core.utils :as u]))


(defrecord SimpleVersion [number distance sha dirty?]
  Object
  (toString [_]
    (-> (str number)
        (cond-> (pos? distance) (str "-" distance "-g" sha)
                dirty? (str "-DIRTY")))))


(def initial-simple-version (SimpleVersion. 0 0 "" false))


(defn parse-version-number [s]
  (Integer/parseInt s))

(u/simple-fdef parse-version-number
               string?
               :simple-version/number)


(defn simple-version [x]
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

