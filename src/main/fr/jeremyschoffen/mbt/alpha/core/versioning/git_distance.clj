(ns ^{:author "Jeremy Schoffen"
      :doc "
Building blocks of a versioning system based on git commit distance.
      "}
  fr.jeremyschoffen.mbt.alpha.core.versioning.git-distance
  (:require
    [cognitect.anomalies :as anom]
    [fr.jeremyschoffen.mbt.alpha.core.specs]
    [fr.jeremyschoffen.mbt.alpha.utils :as u]))


(defrecord SimpleVersion [number distance sha dirty?]
  Object
  (toString [_]
    (-> (str number)
        (cond-> (pos? distance) (str "-" distance "-g" sha)
                dirty? (str "-DIRTY")))))


(def initial-simple-version
  "Initial value to use when starting the versioning process from scratch."
  (SimpleVersion. 0 0 "" false))


(def version-regex #"(\d)+(-unstable)?")


(defn parse-version [s]
  (let [[_ n unstable] (re-matches version-regex s)]
    {:number (Integer/parseInt n)
     :stable (not unstable)}))

(u/simple-fdef parse-version
               string?
               :simple-version/number)


(defn git-distance-version
  "Make a representation of a version in a simple style.
  Here the version is ultimately just 1 number counting the number of commits since version 0."
  [x]
  (map->SimpleVersion x))

(u/spec-op git-distance-version
           :param {:req-un [:simple-version/number
                            :git.describe/distance
                            :git/sha
                            :git.repo/dirty?]})


(defn- throw-duplicating []
  (throw (ex-info "Duplicating tag."
                  {::anom/category ::anom/forbidden
                   :mbt/error :versioning/duplicating-tag})))

(defn bump [v & _]
  (let [{:keys [number distance sha dirty?]} v]
    (when (zero? distance)
      (throw-duplicating))
    (SimpleVersion. (+ number distance) 0 sha dirty?)))