(ns ^{:author "Jeremy Schoffen"
      :doc "
Api containing the default logic for using git state as a versioning mechanism.
      "}
  fr.jeremyschoffen.mbt.alpha.default.versioning.git-state
  (:require
    [clojure.spec.alpha :as s]
    [cognitect.anomalies :as anom]
    [fr.jeremyschoffen.java.nio.alpha.file :as fs]
    [fr.jeremyschoffen.mbt.alpha.core :as mbt-core]
    [fr.jeremyschoffen.mbt.alpha.core.specs]
    [fr.jeremyschoffen.mbt.alpha.default.specs]
    [fr.jeremyschoffen.mbt.alpha.default.versioning.schemes :as vs]
    [fr.jeremyschoffen.mbt.alpha.utils :as u])
  (:import [java.util Date TimeZone]
           [java.text SimpleDateFormat]))

(u/pseudo-nss
  git
  git.describe
  git.tag
  project
  versioning)


;;----------------------------------------------------------------------------------------------------------------------
;; Versioning
;;----------------------------------------------------------------------------------------------------------------------
(defn check-some-commit
  "Check whether the repo has any commit or not. Throws an exception if not."
  [param]
  (when-not (mbt-core/git-any-commit? param)
    (throw (ex-info "No commits  found."
                    (merge param {::anom/category ::anom/not-found
                                  :mbt/error      :no-commit})))))

(u/spec-op check-some-commit
           :deps [mbt-core/git-any-commit?]
           :param {:req [::git/repo]})


(defn most-recent-description
  "Get the most recent description for the tags named with the base `:versioning/tag-base-name`."
  [{repo ::git/repo
    tag-base ::versioning/tag-base-name
    :as param}]

  (check-some-commit param)
  (let [pattern (if tag-base
                  (str tag-base "-v*")
                  "*")]
    (mbt-core/git-describe {::git/repo                 repo
                            ::git.describe/tag-pattern pattern})))

(u/spec-op most-recent-description
           :deps [mbt-core/git-describe]
           :param {:req [::git/repo]
                   :opt [::versioning/tag-base-name]}
           :ret (s/nilable ::git/description))


(defn current-version
  "Get the current version using the provided version scheme."
  [param]
  (when-let [desc (most-recent-description param)]
    (-> param
        (assoc ::git/description desc)
        (vs/current-version))))

(u/spec-op current-version
           :deps [most-recent-description vs/current-version]
           :param {:req [::git/repo
                         ::versioning/scheme]
                   :opt [::versioning/tag-base-name]}
           :ret (s/nilable ::versioning/version))


(defn next-version
  "Get the next version using the provided version scheme."
  [param]
  (if-let [desc (most-recent-description param)]
    (-> param
        (assoc ::git/description desc)
        (u/assoc-computed ::versioning/version vs/current-version)
        vs/bump)
    (vs/initial-version param)))

(u/spec-op next-version
           :deps [most-recent-description vs/current-version vs/bump]
           :param {:req [::git/repo
                         ::versioning/scheme]
                   :opt [::versioning/tag-base-name
                         ::versioning/bump-level]}
           :ret ::versioning/version)


;;----------------------------------------------------------------------------------------------------------------------
;; Buidling tags
;;----------------------------------------------------------------------------------------------------------------------
(defn- tag-name [{base ::versioning/tag-base-name
                  v    ::versioning/version}]
  (str base "-v" v))

(u/spec-op tag-name
           :param {:req [::versioning/tag-base-name
                         ::versioning/version]}
           :ret ::git.tag/name)

;; taken from https://github.com/jgrodziski/metav/blob/master/src/metav/domain/metadata.clj#L8
(defn- iso-now []
  (let [tz (TimeZone/getTimeZone "UTC")
        df (SimpleDateFormat. "yyyy-MM-dd'T'HH:mm:ss'Z'")]
    (.setTimeZone df tz)
    (.format df (Date.))))


;; inspired by https://github.com/jgrodziski/metav/blob/master/src/metav/domain/metadata.clj#L15
(defn- make-tag-data [{base ::versioning/tag-base-name
                       v ::versioning/version
                       git-prefix ::git/prefix
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
           :param {:req [::versioning/tag-base-name
                         ::versioning/version
                         ::git/prefix]}
           :ret map?)


(defn- make-tag
  "Create tag data usable by our git wrapper."
  [param]
  (let [m (make-tag-data param)]
    {::git.tag/name (:tag-name m)
     ::git.tag/message (pr-str m)}))

(u/spec-op make-tag
           :deps [make-tag-data]
           :param {:req [::versioning/tag-base-name
                         ::versioning/version
                         ::git/prefix]}
           :ret ::git/tag)


(defn new-tag
  "Make the next tag/milestone."
  [param]
  (-> param
      (u/assoc-computed ::git/prefix mbt-core/git-prefix)
      make-tag))

(u/spec-op new-tag
           :deps [mbt-core/git-prefix make-tag]
           :param {:req [::project/working-dir
                         ::versioning/tag-base-name
                         ::versioning/version]}
           :ret ::git/tag)


;;----------------------------------------------------------------------------------------------------------------------
;; Operations!
;;----------------------------------------------------------------------------------------------------------------------
(defn check-not-dirty
  "Check wheter the git repo is dirty or not. Throws if it is."
  [param]
  (when (mbt-core/git-dirty? param)
    (throw (ex-info "Can't do this operation on a dirty repo."
                    (merge param {::anom/category ::anom/forbidden
                                  :mbt/error :dirty-repo})))))

(u/spec-op check-not-dirty
           :deps [mbt-core/git-dirty?]
           :param {:req [::git/repo]})


(defn check-repo-in-order
  "Concentrate several checks in one function. Namely:
    - [[fr.jeremyschoffen.mbt.alpha.default.versioning.git-state/check-some-commit]]
    - [[fr.jeremyschoffen.mbt.alpha.default.versioning.git-state/check-not-dirty]]"
  [ctxt]
  (-> ctxt
      (u/check check-some-commit)
      (u/check check-not-dirty)))

(u/spec-op check-repo-in-order
           :deps [check-some-commit check-not-dirty]
           :param {:req [::git/repo]})


(defn tag!
  "Create a new git tag provided [[fr.jeremyschoffen.mbt.alpha.default.versioning.git-state/check-repo-in-order]]
  passes."
  [param]
  (-> param
      (u/check check-repo-in-order)
      mbt-core/git-tag!))

(u/spec-op tag!
           :deps [check-repo-in-order mbt-core/git-tag!]
           :param {:req [::git/repo
                         ::git/tag!]})


(defn tag-new-version!
  "Create a new tag in git marking a new milestone in the project."
  [param]
  (-> param
      (u/augment-computed ::git/tag! new-tag)
      tag!))

(u/spec-op tag-new-version!
           :deps [new-tag tag!]
           :param {:req [::git/repo
                         ::project/working-dir
                         ::versioning/tag-base-name
                         ::versioning/version]}
           :ret ::git/tag)