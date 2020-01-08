;;; An implementation of Maven version 3 (borrowed from [lein-v](https://github.com/roomkey/lein-v))
;;; It supports three levels of numeric versions (major, minor & patch).  Commit distance
;;; is represented by the Maven build number (e.g. 1.2.3-9) when there is no qualifier, and
;;; by a trailing, dash-separated numeric qualifier (practically, a build number) in the presence
;;; of a qualifier.  Qualifiers may have up to nine releases, e.g. beta3, alpha4 and are considered
;;; one-based with the first release (release 1) not printing its release number.
;;; SNAPSHOT qualifiers are allowed and when a SNAPSHOT version is in effect, commit
;;; distance as a build number is suppressed.  Effectively many commits can have the same
;;; (SNAPSHOT) version.
;;; http://maven.apache.org/ref/3.2.5/maven-artifact/index.html#
;;; http://maven.apache.org/ref/3.3.9/maven-artifact/apidocs/org/apache/maven/artifact/versioning/ComparableVersion.html
;;; https://cwiki.apache.org/confluence/display/MAVENOLD/Versioning
;;; Example Versions & Interpretations:
;;; 1.2.3-rc4 => major 1, minor 2, patch 3, qualifier rc incremented to 4
;;; NB: java -jar ~/.m2/repository/org/apache/maven/maven-artifact/3.2.5/maven-artifact-3.2.5.jar <v1> <v2> ...<vn>

(ns com.jeremyschoffen.mbt.api.versioning.version.metav.maven
  "An implementation of version protocols that complies with Maven v3"
  (:require [clojure.spec.alpha :as s]
            [clojure.string :as string]
            [com.jeremyschoffen.mbt.api.versioning.version.protocols :as vp]
            [com.jeremyschoffen.mbt.api.versioning.version.common :as common]
            [com.jeremyschoffen.mbt.api.versioning.version.metav.common :as metav-common])
  (:import [java.lang Comparable]
           [org.apache.maven.artifact.versioning DefaultArtifactVersion]))

;; not allowing snapshots, they might duplicate git tag names.
(def allowed-bumps #{:major :minor :patch :alpha :beta :rc :release})


(s/def ::accepted-bumps allowed-bumps)


(defn- string->qualifier
  [qstring]
  (let [[_ base i] (re-matches #"(\D+)(\d)*" qstring)
        i (or (and i (Integer/parseUnsignedInt i)) 1)] ; => max revisions = 9
    [base i]))


(defn- qualifier->string
  [[qs qn]]
  (str qs (when (> qn 1) qn)))


(defn- qualify*
  [[qs qn] qualifier]
  (if (and (= qs qualifier) (not= qs "SNAPSHOT")) [qs (inc qn)] [qualifier 1]))


(defn- to-string [subversions qualifier & [distance sha dirty?]]
  (cond-> (string/join "." subversions)
    qualifier (str "-" (qualifier->string qualifier))
    (and distance (pos? distance)) (str "-" distance "-0x" sha)
    dirty? (str "-DIRTY")))


(deftype MavenVersion [subversions qualifier distance sha dirty?]
  Object
  (toString [_] (to-string subversions qualifier distance sha dirty?))
  Comparable
  (compareTo [_ that] ; Need to suppress SHA for purposes of comparison
    (if (instance? MavenVersion that)
      (let [that ^MavenVersion that]
        (compare (DefaultArtifactVersion. (to-string subversions qualifier distance nil dirty?))
                 (let [subversions (.subversions that)
                       qualifier (.qualifier that)
                       distance (.distance that)
                       dirty? (.dirty? that)]
                   (DefaultArtifactVersion. (to-string subversions qualifier distance nil dirty?)))))
      (throw (IllegalArgumentException. (format "Can't compare a MavenVersion with %s." that)))))
  metav-common/SCMHosted
  (subversions [_] subversions)
  (tag [_] (to-string subversions qualifier))
  (distance [_] distance)
  (sha [_] sha)
  (dirty? [_] dirty?)
  metav-common/Bumpable
  (bump* [_ level]
    (condp contains? level
      #{:major :minor :patch} (let [subversions (metav-common/bump-subversions subversions level)]
                                (MavenVersion. (vec subversions) nil 0 sha dirty?))

      #{:alpha :beta :rc} (MavenVersion. subversions (qualify* qualifier (name level)) 0 sha dirty?)

      #{:snapshot} (MavenVersion. subversions (qualify* qualifier "SNAPSHOT") 0 sha dirty?)

      #{:release} (do (assert qualifier "There is no pre-bump version pending")
                      (MavenVersion. subversions nil 0 sha dirty?))

      (throw (Exception. (str "Not a supported bump operation: " level))))))

(def tag-pattern #".*(\d+.\d+.\d+)(?:-(.*))?$")
(defn- parse-tag [vstring]
  (println)
  (let [[_ subs q] (re-matches tag-pattern vstring)
        subversions (into [] (map #(Integer/parseInt %)) (string/split subs #"\."))
        qualifier (and q (string->qualifier q))]
    [subversions qualifier]))

(defn version
  ([] (MavenVersion. metav-common/default-initial-subversions nil nil 0 nil))
  ([tag distance sha dirty?]
   (if tag
     (let [[subversions qualifier] (parse-tag tag)]
       (MavenVersion. subversions qualifier distance sha dirty?))
     (MavenVersion. metav-common/default-initial-subversions nil distance sha dirty?))))

(defn- current-version* [param]
  (let [{tag :git.tag/name
         dist :git.describe/distance
         sha :git/sha
         dirty? :git.repo/dirty?} (common/most-recent-description param)]
    (version tag dist sha dirty?)))

(def version-scheme
  (reify vp/VersionScheme
    (initial-version [_]
      (version))
    (current-version [_ state]
      (current-version* state))
    (bump [_ version]
      (metav-common/safer-bump version :patch))
    (bump [_ version level]
      (metav-common/safer-bump version level))))