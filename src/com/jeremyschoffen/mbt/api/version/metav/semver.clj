;;; An implementation of Semantic Version 2.0.0. (borrowed from [lein-v(https://github.com/roomkey/lein-v)])
;;; It supports major and minor releases and implicit patch releases based on the commit
;;; distance from the last major/minor version-tagged commit.  SCM tags are, however, of the
;;; form "major.minor.0" to be more aesthetically pleasing and consistent with the standard.
;;; SHA metadata is added for positive commit distances, and a "DIRTY" metadatum is added when
;;; appropriate.  There is no support for Semantic Version's pre-releases!  The ordering/
;;; precedence rules cannot be reconciled with the automatic assignment of patch releases.
;;; http://semver.org/spec/v2.0.0.html
(ns com.jeremyschoffen.mbt.api.version.metav.semver
  "An implementation of version protocols that complies with Semantic Versioning 2.0.0"
  (:require
   [clojure.string :as string]
   [clojure.spec.alpha :as s]
   [clojure.tools.logging :as log]
   [com.jeremyschoffen.mbt.api.version.metav.protocols :refer [SCMHosted Bumpable]]
   [com.jeremyschoffen.mbt.api.version.metav.common :as common]))



(def allowed-bumps #{:major :minor :patch})


(s/def ::accepted-bumps allowed-bumps)


(deftype SemVer [subversions distance sha dirty?]
  Object
  (toString [this] (let [be (string/join "." subversions); (conj subversions distance))
                         _ (log/debug "be" be)
                         metadata (string/join "." (cond-> []
                                                     (and distance (pos? distance)) (conj (str "0x" sha))
                                                     dirty? (conj "DIRTY")))
                         _ (log/debug "metadata" metadata)]
                     (cond-> be
                       (not (string/blank? metadata)) (str "-" metadata))))
  Comparable
  (compareTo [this that]
    (if (instance? SemVer that)
      (let [that ^SemVer that]
        (compare [(vec (.subversions this)) (.distance this) (.dirty? this)]
                 [(vec (.subversions that)) (.distance that) (.dirty? that)]))
      (throw (IllegalArgumentException. (format "Can't compare a SemVer with %s." that)))))
  SCMHosted
  (subversions [this] subversions)
  (tag [this] (string/join "." (conj subversions distance)))
  (distance [this] distance)
  (sha [this] sha)
  (dirty? [this] dirty?)
  Bumpable
  (bump [this level]
    (condp contains? level
      #{:major :minor :patch} (let [subversions (common/bump-subversions subversions level)]
                                (SemVer. (vec subversions) 0 sha dirty?))
      ;#{:patch} (throw (Exception. "Patch bump are implicit by commit distance"))
      (throw (Exception. (str "Not a supported bump operation: " level))))))


(let [re #"(\d+)\.(\d+)\.(\d+)"]
  (defn- parse-base [base]
    (log/debug "parse-base(" base ")")
    (let [[_ major minor patch] (re-find re base)]
      ;(assert (= "0" patch) (str "Non-zero patch level (" patch ") found in SCM base"))
      (mapv #(Integer/parseInt %) [major minor patch]))))


(defn version
  ([] (SemVer. common/default-initial-subversions 0 nil nil))
  ([base distance sha dirty?]
   (if base
     (let [subversions (parse-base base)]
       (SemVer. subversions distance sha dirty?))
     (SemVer. common/default-initial-subversions distance sha dirty?))))
