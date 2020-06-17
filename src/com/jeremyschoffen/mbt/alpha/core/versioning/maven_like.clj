(ns com.jeremyschoffen.mbt.alpha.core.versioning.maven-like
  (:require [clojure.spec.alpha :as s]
            [clojure.string :as string]
            [cognitect.anomalies :as anom])
  (:import [java.lang Comparable]
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


(def allowed-qualifiers #{:alpha :beta :rc})


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
  (let [parsed (DefaultArtifactVersion. s)]
    {:subversions [(.getMajorVersion parsed)
                   (.getMinorVersion parsed)
                   (.getIncrementalVersion parsed)]
     :qualifier (some-> parsed .getQualifier parse-qualifier)}))


;;----------------------------------------------------------------------------------------------------------------------
;; Version -> string
;;----------------------------------------------------------------------------------------------------------------------
(defn qualifier->str [{:keys [label n]}]
  (str (name label)
       (when (> n 1)
         n)))


(defn- bases-str [{:keys [subversions qualifier]}]
  (str (string/join "." subversions)
       (when qualifier
         (str "-" (qualifier->str qualifier)))))


(defn- git-str [{:keys [distance sha dirty?]}]
  (->> [distance
        (str "g" sha)
        (when dirty? "DIRTY")]
       (remove nil?)
       (string/join "-")))


(defn- to-string [{:keys [distance] :as v}]
  (let [suffix (git-str v)]
    (str (bases-str v)
         (when (and distance (pos? distance))
           (str "-" suffix)))))


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


(defn bump-maven* [v level]
  (condp contains? level
    #{:major :minor :patch} (bump-subversions v level)
    #{:alpha :beta :rc} (bump-qualifier v level)
    #{:release} (bump-release v)
    (not-supported level)))


(defn reset-distance [v]
  (assoc v :distance 0))


(defn bump-maven [v level]
  (-> v
      (bump-maven* level)
      reset-distance))

;; TODO: take qualifiers into account ex beta -> beta2 but no changes
(defn duplicating-version? [{:keys [subversions distance qualifier]} level]
  (when (zero? distance)
    (let [[_ minor patch] subversions
          same-patch? (contains? (conj allowed-qualifiers :patch) level)
          same-minor? (and (= level :minor)
                           (= patch 0))
          same-major? (and (= level :major)
                           (= patch 0)
                           (= minor 0))]
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


(def initial-version "0.1.0")


(defn maven-version
  ([]
   (-> initial-version parse-version maven-version))
  ([x]
   (let [v (map->MavenVersion x)
         label (get-in v [:qualifier :label])]
     (if (and label
              (not (contains? allowed-qualifiers label)))
       (throw (ex-info (str "Unsupported qualifier: " label)
                       {::anom/category ::anom/unsupported
                        :qualifier label}))
       v))))


(defrecord SemverVersion [subversions qualifier distance sha dirty?]
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
  ([]
   (-> initial-version parse-version semver-version))
  ([x]
   (-> x
       (dissoc :qualifier)
       map->SemverVersion)))