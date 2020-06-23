(ns com.jeremyschoffen.mbt.alpha.default.versioning.git-state
  (:require
    [clojure.spec.alpha :as s]
    [cognitect.anomalies :as anom]
    [com.jeremyschoffen.java.nio.file :as fs]

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
(defn check-some-commit [param]
  (when-not (git/any-commit? param)
    (throw (ex-info "No commits  found."
                    (merge param {::anom/category ::anom/not-found
                                  :mbt/error      :no-commit})))))

(u/spec-op check-some-commit
           :deps [git/any-commit?]
           :param {:req [:git/repo]})


(defn most-recent-description [{repo :git/repo
                                tag-base :versioning/tag-base-name
                                :as param}]
  (check-some-commit param)
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
  (let [prefix (str git-prefix)
        path (if (empty? prefix)
               "."
               prefix)]
    {:name base
     :version (str v)
     :tag-name (tag-name param)
     :generated-at (iso-now)
     :path path}))

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
        :versioning/version next-version)
      tag))

(u/spec-op next-tag
           :deps [git/prefix names/tag-base-name tag next-version]
           :param {:req #{:git/repo
                          :project/working-dir
                          :versioning/tag-base-name
                          :versioning/scheme}
                   :opt #{:versioning/bump-level}}
           :ret :git/tag)


;;----------------------------------------------------------------------------------------------------------------------
;; Operations!
;;----------------------------------------------------------------------------------------------------------------------
(def module-build-file "deps.edn")


(defn has-build-file?
  "Checking that the working dir contains a `deps.edn` file."
  [{wd :project/working-dir}]
  (let [build-file (u/safer-path wd module-build-file)]
    (fs/exists? build-file)))

(u/spec-op has-build-file?
           :param {:req [:project/working-dir]}
           :ret boolean?)


(defn check-build-file [param]
  (when-not (has-build-file? param)
    (throw (ex-info "No build file detected."
                    (merge param {::anom/category ::anom/not-found
                                  :mbt/error      :no-build-file})))))

(u/spec-op check-build-file
           :deps [has-build-file?]
           :param {:req [:project/working-dir]})


(defn check-not-dirty [param]
  (when (git/dirty? param)
    (throw (ex-info "Can't do this operation on a dirty repo."
                    (merge param {::anom/category ::anom/forbidden
                                  :mbt/error :dirty-repo})))))

(u/spec-op check-not-dirty
           :deps [git/dirty?]
           :param {:req [:git/repo]})


(defn check-repo-in-order
  "Checks that the working dir has a build file and is in a repo
  which already has at least one commit."
  [ctxt]
  (-> ctxt
      (u/check check-some-commit)
      (u/check check-build-file)
      (u/check check-not-dirty)))

(u/spec-op check-repo-in-order
           :deps [check-build-file check-some-commit check-not-dirty]
           :param {:req [:project/working-dir :git/repo]})


(defn tag! [param]
  (-> param
      (u/check check-repo-in-order)
      git/create-tag!))

(u/spec-op tag!
           :deps [ git/create-tag!]
           :param {:req [:git/repo :git/tag!]})
