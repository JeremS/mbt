(ns com.jeremyschoffen.mbt.alpha.versioning.git-state
  (:require
    [clojure.spec.alpha :as s]
    [clojure.tools.logging :as log]
    [cognitect.anomalies :as anom]
    [com.jeremyschoffen.java.nio.file :as fs]
    [com.jeremyschoffen.mbt.alpha.git :as git]
    [com.jeremyschoffen.mbt.alpha.specs]
    [com.jeremyschoffen.mbt.alpha.utils :as u]
    [com.jeremyschoffen.mbt.alpha.versioning.schemes :as vs])

  (:import [java.util Date TimeZone]
           [java.text SimpleDateFormat]))

;;----------------------------------------------------------------------------------------------------------------------
;; basic state
;;----------------------------------------------------------------------------------------------------------------------
(defn basic-git-state [param]
  (u/assoc-computed param
    :git/top-level git/top-level
    :git/prefix git/prefix
    :git/repo git/make-jgit-repo))

(u/spec-op basic-git-state
           (s/keys :req [:project/working-dir])
           :git/basic-state)

;;----------------------------------------------------------------------------------------------------------------------
;; Versioning
;;----------------------------------------------------------------------------------------------------------------------
(defn- most-recent-description [{repo :git/repo
                                 artefact-name :artefact/name}]
  (git/describe {:git/repo repo
                 :git.describe/tag-pattern (str artefact-name "*")}))

(u/spec-op most-recent-description
           (s/keys :req [:git/repo :artefact/name])
           (s/nilable :git/description))


(defn current-version [param]
  (when-let [desc (most-recent-description param)]
    (-> param
        (assoc :git/description desc)
        (vs/current-version))))

(u/spec-op current-version
           (s/keys :req [:git/repo :artefact/name :version/scheme])
           (s/nilable :project/version))


(defn next-version [param]
  (if-let [desc (most-recent-description param)]
    (-> param
        (assoc :git/description desc)
        (u/assoc-computed :project/version vs/current-version)
        vs/bump)
    (vs/initial-version param)))

(u/spec-op next-version
           (s/keys :req [:git/repo :artefact/name :version/scheme])
           :project/version)

;;----------------------------------------------------------------------------------------------------------------------
;; Buidling tags
;;----------------------------------------------------------------------------------------------------------------------
(defn tag-name [{artefact-name :artefact/name
                 v             :project/version}]
  (str artefact-name "-v" v))

(u/spec-op tag-name
           (s/keys :req [:artefact/name :project/version])
           :git.tag/name)


(defn- iso-now []
  (let [tz (TimeZone/getTimeZone "UTC")
        df (SimpleDateFormat. "yyyy-MM-dd'T'HH:mm:ss'Z'")]
    (.setTimeZone df tz)
    (.format df (Date.))))


(defn make-tag-data [{artefact-name :artefact/name
                      v             :project/version
                      git-prefix    :git/prefix
                      :as           param}]
  {:artefact-name artefact-name
   :version       (str v)
   :tag           (tag-name param)
   :generated-at  (iso-now)
   :path          (or (and git-prefix (str git-prefix))
                      ".")})

(u/spec-op make-tag-data
           (s/keys :req [:artefact/name
                         :project/version
                         :git/prefix])
           map?)


(defn tag
  "Creates tag data usable by our git wrapper."
  [param]
  (let [m (make-tag-data param)]
    {:git.tag/name (:tag m)
     :git.tag/message (pr-str m)}))

(u/spec-op tag
           (s/keys :req [:artefact/name
                         :project/version
                         :git/prefix])
           :git/tag)


(defn next-tag [param]
  (-> param
      (u/assoc-computed :project/version next-version)
      tag))


(u/spec-op next-tag
           (s/keys :req [:artefact/name :git/repo :git/prefix])
           :git/tag)

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
           (s/keys :req [:project/working-dir])
           boolean?)


(defn check-repo-in-order
  "Checks that the working dir has a build file and is in a repo
  which already has at least one commit."
  [context]
  (when-not (git/any-commit? context)
    (let [msg "No commits  found."
          e (ex-info msg (merge context {::anom/category ::anom/forbidden
                                         :mbt/error :no-commit}))]
      (log/fatal e msg)
      (throw e)))
  (when-not (has-build-file? context)
    (let [msg "No build file detected."
          e (ex-info msg (merge context {::anom/category ::anom/forbidden
                                         :mbt/error :no-build-file}))]
      (log/fatal e msg)
      (throw e)))

  context)

(u/spec-op check-repo-in-order
           (s/keys :req [:project/working-dir :git/repo]))


(defn check-not-dirty [param]
  (when (git/dirty? param)
    (throw (ex-info "Can't do this operation on a dirty repo."
                    (merge param {::anom/category ::anom/forbidden
                                  :mbt/error :dirty-repo}))))
  param)


(defn- tag!
  "Checks that the repo is in order and creates a git tag."
  [param]
  (-> param
      check-repo-in-order
      check-not-dirty
      git/create-tag!))

(u/spec-op tag!
           (s/merge (s/keys :req [:git/repo])
                    :git/tag)
           :git/tag)


(defn bump-tag!
  "Creates a new tag for the current commit bumping the version number for the selected artefact-name."
  [param]
  (-> param
      (u/merge-computed next-tag)
      tag!))

(u/spec-op bump-tag!
           (s/keys :req [:artefact/name :git/repo :git/prefix])
           :git/tag)


