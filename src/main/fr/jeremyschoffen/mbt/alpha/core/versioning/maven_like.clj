(ns ^{:author "Jeremy Schoffen"
      :doc "
Building blocks to versioning systems following the maven or semver model.
      "}
  fr.jeremyschoffen.mbt.alpha.core.versioning.maven-like
  (:require
    [clojure.spec.alpha :as s]
    [clojure.string :as string]
    [cognitect.anomalies :as anom]
    [fr.jeremyschoffen.mbt.alpha.core.specs :as specs]
    [fr.jeremyschoffen.mbt.alpha.utils :as u])
  (:import
    [java.lang Comparable]
    [org.apache.maven.artifact.versioning DefaultArtifactVersion]))

;;----------------------------------------------------------------------------------------------------------------------
;; Parsing versions
;;----------------------------------------------------------------------------------------------------------------------
(def qualifer-regex #"([^\d-]+)(\d*).*")


(defn- make-qualifier
  ([label]
   (make-qualifier label 1))
  ([label n]
   {:label label
    :n n}))


(defn- parse-qualifier [s]
  (let [[_ q n] (re-matches qualifer-regex s)]
    (when q
      (let [label (keyword q)
            n (and n
                   (seq n)
                   (Integer/parseInt n))]
        (if n
          (make-qualifier label n)
          (make-qualifier label))))))


(defn parse-version [s]
  (let [parsed (DefaultArtifactVersion. s)
        qualifier (some-> parsed .getQualifier parse-qualifier)]
    (cond-> {:subversions [(.getMajorVersion parsed)
                           (.getMinorVersion parsed)
                           (.getIncrementalVersion parsed)]}
            qualifier (assoc :qualifier qualifier))))


(u/simple-fdef parse-version
               string?
               (s/keys :req-un [:maven-like/subversions]
                       :opt-un [:maven-like/qualifier]))


;;----------------------------------------------------------------------------------------------------------------------
;; Version -> string
;;----------------------------------------------------------------------------------------------------------------------
(defn qualifier->str [{:keys [label n]}]
  (str (name label)
       (when (> n 1)
         n)))


(defn- bases-str [{:keys [subversions qualifier]}]
  (cond-> (string/join "." subversions)
          qualifier (str "-" (qualifier->str qualifier))))


(defn- distance-str [{:keys [distance sha]}]
  (str distance "-g" sha))


(defn- to-string [{:keys [distance dirty?] :as v}]
  (let [distance-part (distance-str v)
        add-distance? (and distance (pos? distance))]
    (cond-> (bases-str v)
            add-distance? (str "-" distance-part)
            dirty?        (str "-DIRTY"))))


;;----------------------------------------------------------------------------------------------------------------------
;; Comparing versions
;;----------------------------------------------------------------------------------------------------------------------
(defn- make-comparable [x]
  (-> x
      bases-str
      DefaultArtifactVersion.))


(defn- compare-versions [v1 v2]
  (compare (make-comparable v1)
           (make-comparable v2)))


;;----------------------------------------------------------------------------------------------------------------------
;; Bumping versions
;;----------------------------------------------------------------------------------------------------------------------
(defprotocol BumpAble
  (bump [this level]))


(defn- reset-qualifier [v]
  (assoc v :qualifier nil))


(defn- bump-subversions* [subversions level]
  (let [[major minor patch] subversions]
    (case level
      :major [(inc major) 0 0]
      :minor [major (inc minor) 0]
      :patch [major minor (inc patch)])))


(defn- bump-subversions [v level]
  (-> v
      (update :subversions bump-subversions* level)
      reset-qualifier))


(defn- bump-qualifier-number [q]
  (update q :n inc))


(defn- bump-qualifier [{:keys [qualifier] :as v} level]
  (let [{:keys [label]} qualifier]
    (if (= label level)
      (update v :qualifier bump-qualifier-number)
      (-> v
          (cond-> (not qualifier)
                  (bump-subversions :patch))
          (assoc :qualifier (make-qualifier level))))))


(defn- bump-release [{:keys [qualifier] :as v}]
  (if qualifier
    (reset-qualifier v)
    (throw (ex-info "There is no pre-bump version pending."
                    {::anom/category ::anom/forbidden}))))


(defn- not-supported [level]
  (throw (ex-info (str "Not a supported bump operation: " level)
                  {::anom/category ::anom/unsupported
                   :version/bump-level level})))


(defn- bump-maven* [v level]
  (condp contains? level
    #{:major :minor :patch} (bump-subversions v level)
    #{:alpha :beta :rc} (bump-qualifier v level)
    #{:release} (bump-release v)
    (not-supported level)))


(defn- reset-distance [v]
  (assoc v :distance 0))


(defn- bump-maven [v level]
  (-> v
      (bump-maven* level)
      reset-distance))


(defn- duplicating-version? [{:keys [subversions distance]} level]
  (when (zero? distance)
    (let [[_ minor patch] subversions
          same-patch? (contains? (conj specs/allowed-qualifiers :patch) level)
          same-minor? (and (= level :minor)
                           (= patch 0))
          same-major? (and (= level :major)
                           (= patch 0)
                           (= minor 0))]
      (or same-patch?
          same-minor?
          same-major?))))


(defn- going-backwards? [old-version new-version]
 (pos? (compare old-version new-version)))


(defn- assert-bump? [old-version level new-version]
  (when (duplicating-version? old-version level)
    (throw (ex-info (str "Aborted released, bumping with level: " level
                         " would create version: " new-version " pointing to the same commit as version: " old-version ".")
                    {::anom/category ::anom/forbidden
                     :mbt/error :versioning/duplicating-tag})))
  (when (going-backwards? old-version new-version)
    (throw (ex-info (str "Can't bump version to an older one : " old-version " -> " new-version " isn't allowed.")
                    {::anom/category ::anom/forbidden
                     :mbt/error :versioning/going-backward}))))


(defn safer-bump
  "Bump a version according to a level :major, :minor, or :patch in the general case and :alpha, :beta, :rc and :release
  in the maven case.

  Using commit distances this function can throw exceptions when the bump would just make another version of the same
  artefact or would in effect progress backward.

  Example:
  - with commit distance 0, bumping from 0.0.1 to 0.0.2 would crete 2 version of the same artefact.
  - bumping from 0.1.0-beta to 0.1.0-alpha would be going backward."
  [v level]
  (let [new-v (bump v level)]
    (assert-bump? v level new-v)
    new-v))


;;----------------------------------------------------------------------------------------------------------------------
;; Implementations
;;----------------------------------------------------------------------------------------------------------------------
(defrecord MavenVersion [subversions qualifier distance sha dirty?]
  Object
  (toString [this] (to-string this))

  Comparable
  (compareTo [this that] (compare-versions this that))

  BumpAble
  (bump [this level] (bump-maven this level)))


(def ^:private initial-version "0.1.0")


(defn maven-version
  "Make a representation of a version in the maven style."
  [x]
  (let [v (map->MavenVersion x)
        label (get-in v [:qualifier :label])]
    (if (and label (not (contains? specs/allowed-qualifiers label)))
      (throw (ex-info (str "Unsupported qualifier: " label)
                      {::anom/category ::anom/unsupported
                       :qualifier label}))
      v)))

(u/spec-op maven-version
           :param {:req-un [:maven-like/subversions]
                   :opt-un [:maven-like/qualifier
                            :git.describe/distance
                            :git/sha
                            :git.repo/dirty?]})

(def initial-maven-version
  "Initial value to use when starting the versioning process from scratch."
  (-> initial-version parse-version maven-version))


(defrecord SemverVersion [subversions distance sha dirty?]
  Object
  (toString [this] (to-string this))

  Comparable
  (compareTo [this that] (compare-versions this that))

  BumpAble
  (bump [this level]
    (if (contains? #{:major :minor :patch} level)
      (bump-maven this level)
      (not-supported level))))


(defn semver-version
  "Make a representation of a version in the semver style."
  [x]
  (-> x
      (dissoc :qualifier)
      map->SemverVersion))

(u/spec-op semver-version
           :param {:req-un [:maven-like/subversions]
                   :opt-un [:git.describe/distance
                            :git/sha
                            :git.repo/dirty?]})


(def initial-semver-version
  "Initial value to use when starting the versioning process from scratch."
  (-> initial-version parse-version semver-version))
