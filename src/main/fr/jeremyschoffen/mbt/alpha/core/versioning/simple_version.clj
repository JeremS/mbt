(ns ^{:author "Jeremy Schoffen"
      :doc "
Building blocks of a versioning system based on git commit distance.
      "}
  fr.jeremyschoffen.mbt.alpha.core.versioning.simple-version
  (:require
    [cognitect.anomalies :as anom]
    [fr.jeremyschoffen.mbt.alpha.core.specs]
    [fr.jeremyschoffen.mbt.alpha.utils :as u]))


(defrecord SimpleVersion [number distance sha dirty? stable]
  Object
  (toString [_]
    (-> (str number)
        (cond-> (not stable) (str "-unstable")
                (pos? distance) (str "-" distance "-g" sha)
                dirty? (str "-DIRTY")))))


(def initial-simple-version
  "Initial value to use when starting the versioning process from scratch."
  (SimpleVersion. 0 0 "" false false))


(def version-regex #"(\d)+(-unstable)?")


(defn parse-version [s]
  (let [[_ n unstable] (re-matches version-regex s)]
    {:number (Integer/parseInt n)
     :stable (not unstable)}))

(u/simple-fdef parse-version
               string?
               :simple-version/number)


(defn simple-version
  "Make a representation of a version in a simple style.
  Here the version is ultimately just 1 number counting the number of commits since version 0."
  [x]
  (map->SimpleVersion x))

(u/spec-op simple-version
           :param {:req-un [:simple-version/number
                            :simple-version/stable
                            :git.describe/distance
                            :git/sha
                            :git.repo/dirty?]})


(defn- throw-duplicating []
  (throw (ex-info "Duplicating tag."
                  {::anom/category ::anom/forbidden
                   :mbt/error :versioning/duplicating-tag})))


(defn throw-unsupported [level]
  (throw (ex-info (str "Simple version doesn't support bumping to this level: " level)
                  {::anom/category ::anom/unsupported
                   :mbt/error :versioning/unsupported-level
                   :level level})))


(defn bump
  ([v]
   (let [{:keys [number distance sha dirty? stable]} v]
     (when (zero? distance) (throw-duplicating))
     (SimpleVersion. (+ number distance) 0 sha dirty? stable)))
  ([v level]
   (cond
     (nil? level)
     (bump v)

     (= :stable level)
     (let [{:keys [number distance sha dirty?]} v]
       (SimpleVersion. (+ number distance) 0 sha dirty? true))

     :else
     (throw-unsupported level))))