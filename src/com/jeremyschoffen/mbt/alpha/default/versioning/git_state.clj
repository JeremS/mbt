(ns com.jeremyschoffen.mbt.alpha.default.versioning.git-state
  (:require
    [clojure.spec.alpha :as s]
    [com.jeremyschoffen.mbt.alpha.core.git :as git]
    [com.jeremyschoffen.mbt.alpha.core.specs]
    [com.jeremyschoffen.mbt.alpha.core.utils :as u]
    [com.jeremyschoffen.mbt.alpha.default.specs]
    [com.jeremyschoffen.mbt.alpha.default.names :as names]
    [com.jeremyschoffen.mbt.alpha.default.versioning.schemes :as vs])
  (:import [java.util Date TimeZone]
           [java.text SimpleDateFormat]))



;;----------------------------------------------------------------------------------------------------------------------
;; Versioning
;;----------------------------------------------------------------------------------------------------------------------
(defn most-recent-description [{repo :git/repo
                                tag-base :versioning/tag-base-name}]
  (git/describe {:git/repo                 repo
                 :git.describe/tag-pattern (str tag-base "*")}))

(u/spec-op most-recent-description
           :deps [git/describe]
           :param {:req [:git/repo]
                   :opt [:versioning/tag-base-name]}
           :ret (s/nilable :git/description))


(defn current-version [param]
  (when-let [desc (most-recent-description param)]
    (-> param
        (assoc :git/description desc)
        (vs/current-version))))

(u/spec-op current-version
           :deps [most-recent-description vs/current-version]
           :param {:req [:git/repo :versioning/scheme]
                   :opt [:versioning/tag-base-name]}
           :ret (s/nilable :versioning/version))



(defn next-version [param]
  (if-let [desc (most-recent-description param)]
    (-> param
        (assoc :git/description desc)
        (u/assoc-computed :versioning/version vs/current-version)
        vs/bump)
    (vs/initial-version param)))

(u/spec-op next-version
           :deps [most-recent-description vs/initial-version vs/bump]
           :param {:req [:git/repo :versioning/scheme]
                   :opt [:versioning/tag-base-name :versioning/bump-level]}
           :ret :versioning/version)


;;----------------------------------------------------------------------------------------------------------------------
;; Buidling tags
;;----------------------------------------------------------------------------------------------------------------------
(defn tag-name [{base :versioning/tag-base-name
                 v    :versioning/version}]
  (str base "-v" v))

(u/spec-op tag-name
           :param {:req [:versioning/tag-base-name
                         :versioning/version]}
           :ret :git.tag/name)


(defn- iso-now []
  (let [tz (TimeZone/getTimeZone "UTC")
        df (SimpleDateFormat. "yyyy-MM-dd'T'HH:mm:ss'Z'")]
    (.setTimeZone df tz)
    (.format df (Date.))))


(defn make-tag-data [{base :versioning/tag-base-name
                      v :versioning/version
                      git-prefix :git/prefix
                      :as param}]
  {:name base
   :version (str v)
   :tag-name (tag-name param)
   :generated-at (iso-now)
   :path (or (and git-prefix (str git-prefix))
             ".")})

(u/spec-op make-tag-data
           :deps [tag-name]
           :param {:req [:versioning/tag-base-name
                         :versioning/version
                         :git/prefix]}
           :ret map?)


(defn tag
  "Creates tag data usable by our git wrapper."
  [param]
  (let [m (make-tag-data param)]
    {:git.tag/name (:tag-name m)
     :git.tag/message (pr-str m)}))


(u/spec-op tag
           :deps [make-tag-data]
           :param {:req [:versioning/tag-base-name
                         :versioning/version
                         :git/prefix]}
           :ret :git/tag)


(defn next-tag [param]
  (-> param
      (u/assoc-computed
        :git/prefix git/prefix
        :versioning/tag-base-name names/tag-base-name
        :versioning/version next-version)
      tag))

(u/spec-op next-tag
           :deps [git/prefix names/tag-base-name tag next-version]
           :param {:req #{:git/repo
                          :project/working-dir
                          :versioning/scheme}
                   :opt #{:versioning/bump-level}}
           :ret :git/tag)
