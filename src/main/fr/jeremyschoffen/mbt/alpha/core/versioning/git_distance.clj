(ns ^{:author "Jeremy Schoffen"
      :doc "
Building blocks of a versioning system based on git commit distance.
      "}
  fr.jeremyschoffen.mbt.alpha.core.versioning.git-distance
  (:require
    [clojure.spec.alpha :as s]
    [cognitect.anomalies :as anom]
    [fr.jeremyschoffen.mbt.alpha.core.specs :as specs]
    [fr.jeremyschoffen.mbt.alpha.utils :as u]))


(defrecord GitDistanceVersion [number qualifier distance sha dirty?]
  Object
  (toString [_]
    (-> (str number)
        (cond-> qualifier (str "-" (name qualifier))
                (pos? distance) (str "-" distance "-g" sha)
                dirty? (str "-DIRTY")))))


(def version-regex #"(\d)+(?:-(.+))?")


(defn parse-version [s]
  (let [[_ n q] (re-matches version-regex s)]
    (cond-> {:number (Integer/parseInt n)}
            q (assoc :qualifier (keyword q)))))

(u/simple-fdef parse-version
               string?
               (s/keys :req-un [:git-distance/number]
                       :opt-un [:git-distance/qualifier]))


(defn git-distance-version
  "Make a representation of a version in the git distance model.
  Versions only have one version number which is the number of commits
  since the first version, starting at 0.

  This versioning system allows for alpha and beta qualifiers. Absence of a qualifier
  means that the project is in a stable state. It can't go back to alpha nor beta.
  For this reason using these qualifiers imply their presence from the initial version."
  [x]
  (map->GitDistanceVersion x))

(u/spec-op git-distance-version
           :param {:req-un [:git-distance/number
                            :git.describe/distance
                            :git/sha
                            :git.repo/dirty?]
                   :opt-un [:git-distance/qualifier]})


(defn- throw-duplicating []
  (throw (ex-info "Duplicating tag."
                  {::anom/category ::anom/forbidden
                   :mbt/error :versioning/duplicating-tag})))


(defn adding-qualifier-to-stable? [q-old q-new]
  (and (nil? q-old)
       (not (nil? q-new))))


(defn qualifiers-going-backward? [q-old q-new]
  (and (= q-old :beta)
       (= q-new :alpha)))


(defn assert-bump [old new]
  (let [q-old (:qualifier old)
        q-new (:qualifier new)]
    (when (or (adding-qualifier-to-stable? q-old q-new)
              (qualifiers-going-backward? q-old q-new))
      (throw (ex-info (format "Str can't go backwards from %s to %s." old new)
                      {::anom/category ::anom/forbidden
                       :mbt/error :versioning/going-backward
                       :old old
                       :new new})))))


(defn- throw-unsuported [level]
  (throw (ex-info (str "Unsuported bump: " level)
                  {::anom/category ::anom/forbidden
                   :mbt/error :versioning/duplicating-tag
                   :versioning/bump-level level})))


(defn bump
  ([v]
   (bump v nil))
  ([v level]
   (let [{:keys [number qualifier distance sha dirty?]} v]
     (cond
       (zero? distance)
       (throw-duplicating)

       (nil? level)
       (GitDistanceVersion. (+ number distance) qualifier 0 sha dirty?)

       (= level :stable)
       (GitDistanceVersion. (+ number distance) nil 0 sha dirty?)

       (specs/git-distance-qualifiers level)
       (let [new-v (GitDistanceVersion. (+ number distance) level 0 sha dirty?)]
         (assert-bump v new-v)
         new-v)

       :else (throw-unsuported level)))))


(defn initial-simple-version
  "Initial value to use when starting the versioning process from scratch."
  ([]
   (GitDistanceVersion. 0 nil 0 "" false))
  ([level]
   (when-not (specs/git-distance-qualifiers level)
     (throw-unsuported level))
   (GitDistanceVersion. 0 level 0 "" false)))