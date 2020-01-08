(ns com.jeremyschoffen.mbt.api.git-state
  (:require
    [clojure.edn :as edn]
    [clojure.spec.alpha :as s]
    [clojure.spec.test.alpha :as st]
    [clojure.tools.logging :as log]
    [cognitect.anomalies :as anom]
    [com.jeremyschoffen.java.nio.file :as fs]
    [com.jeremyschoffen.mbt.api.git :as git]
    [com.jeremyschoffen.mbt.api.specs]
    [com.jeremyschoffen.mbt.api.utils :as u]
    [com.jeremyschoffen.mbt.api.version.protocols :as vp]
    [com.jeremyschoffen.mbt.api.version.common :as version-common])

  (:import [java.util Date TimeZone]
           [java.text SimpleDateFormat]))

;;----------------------------------------------------------------------------------------------------------------------
;; Names
;;----------------------------------------------------------------------------------------------------------------------
(defn project-name [{top-level :git/top-level}]
  (-> top-level
      fs/file-name
      str))

(u/spec-op project-name
           (s/keys :req [:git/top-level]))


(defn module-name [{project-name :project/name
                    prefix :git/prefix}]
  (if prefix
    (->> prefix seq (interpose "-") (apply str))
    project-name))

(u/spec-op module-name
           (s/keys :req [:project/name]
                   :opt [:git/prefix])
           :module/name)


(defn artefact-name [{project-name :project/name
                      module-name :module/name}]
  (if (= project-name module-name)
    project-name
    (str project-name "-" module-name)))

(u/spec-op artefact-name
           (s/keys :req [:project/name :module/name])
           :artefact/name)


;;----------------------------------------------------------------------------------------------------------------------
;; basic state
;;----------------------------------------------------------------------------------------------------------------------
(defn assoc-names [context]
  (u/assoc-computed context
                    :project/name project-name
                    :module/name module-name
                    :artefact/name artefact-name))


(defn project-names [param]
  (-> param
      assoc-names
      (select-keys #{:project/name :module/name :artefact/name})))

(u/spec-op project-names
           (s/keys :req [:git/top-level :git/prefix])
           (s/keys :req [:project/name
                         :module/name
                         :artefact/name]))


(defn basic-git-state [param]
  (u/assoc-computed param
    :git/top-level git/top-level
    :git/prefix git/prefix
    :git/repo git/make-jgit-repo))

(u/spec-op basic-git-state (s/keys :req [:project/working-dir]) :git/basic-state)


(defn get-state [param]
  (-> param
      basic-git-state
      assoc-names))

(u/spec-op get-state
           (s/keys :req [:project/working-dir])
           (s/merge :git/basic-state
                    (s/keys :req [:project/name
                                  :module/name
                                  :artefact/name])))

;;----------------------------------------------------------------------------------------------------------------------
;; version
;;----------------------------------------------------------------------------------------------------------------------
(defn current-version [{s :version/scheme :as param}]
  (vp/current-version s param))

(u/spec-op current-version
           (s/keys :req [:version/scheme]))


(defn initial-version [{h :version/scheme}]
  (vp/initial-version h))

(u/spec-op initial-version
           (s/keys :req [:version/scheme]))


(defn bump [{s :version/scheme
             v :project/version
             l :version/bump-level}]
  (if l
    (vp/bump s v l)
    (vp/bump s v)))

(u/spec-op bump
           (s/keys :req [:version/scheme :project/version]
                   :opt [:version/bump-level]))
;;----------------------------------------------------------------------------------------------------------------------
;; Tags
;;----------------------------------------------------------------------------------------------------------------------
(defn tag-name [{artefact-name :artefact/name
                 v             :project/version}]
  (str artefact-name "-v" v))

(u/spec-op tag-name
           (s/keys :req [:artefact/name :project/version]))


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

(defn tag-message [param]
  (pr-str (make-tag-data param)))


(u/spec-op tag-message
           (s/keys :req [:artefact/name
                         :project/version
                         :git/prefix]))

(defn tag [param]
  (let [m (make-tag-data param)]
    {:git.tag/name (:tag m)
     :git.tag/message (pr-str m)}))

(u/spec-op tag
           (s/keys :req [:artefact/name
                         :project/version
                         :git/prefix])
           :git/tag)

;;----------------------------------------------------------------------------------------------------------------------
;; Checks
;;----------------------------------------------------------------------------------------------------------------------
(def module-build-file "deps.edn")


(defn has-build-file?
  "Checking that the working dir contains a `deps.edn` file."
  [{wd :project/working-dir}]
  (let [build-file (u/safer-path wd module-build-file)]
    (fs/exists? build-file)))

(u/spec-op has-build-file? (s/keys :req [:project/working-dir]))


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

;;----------------------------------------------------------------------------------------------------------------------
;; Operations!
;;----------------------------------------------------------------------------------------------------------------------
(defn tag! [param]
  (-> param
      check-repo-in-order
      check-not-dirty
      git/create-tag!))

(u/spec-op tag!
           (s/merge (s/keys :req [:git/repo]) :git/tag)
           :git/tag)


(defn- check-not-initialized [{repo :git/repo
                               artefact-name :artefact/name
                               :as param}]
  (when (try
          (version-common/most-recent-description param)
          (catch Exception _
            nil))
    (throw (ex-info (format "Already started versioning %s." artefact-name)
                    (merge param {::anom/category ::anom/forbidden
                                  :mbt/error :already-tagged}))))
  param)

(u/spec-op check-not-initialized
           (s/keys :req [:git/repo :artefact/name]))


(defn create-first-tag! [param]
  (-> param
      check-not-initialized
      (u/assoc-computed :project/version initial-version)
      (u/merge-computed tag)
      tag!))

(u/spec-op create-first-tag!
           (s/keys :req [:artefact/name :git/repo :git/prefix])
           :git/tag)


(defn bump-tag! [param]
  (-> param
      (u/assoc-computed :project/version current-version)
      (u/assoc-computed :project/version bump)
      (u/merge-computed tag)
      tag!))

(u/spec-op bump-tag!
           (s/keys :req [:artefact/name :git/repo :git/prefix])
           :git/tag)

(comment
  (-> (get-state {:project/working-dir (u/safer-path)})
      (u/assoc-computed :artefact/name artefact-name)
      (u/merge-computed make-initial-tag)
      :git.tag/message
      clojure.edn/read-string))
