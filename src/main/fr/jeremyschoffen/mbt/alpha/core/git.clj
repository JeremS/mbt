(ns ^{:author "Jeremy Schoffen"
      :doc "
Api providing git utilities. Mostly a wrapper for some functionality from `clj-jgit`.
      "}
  fr.jeremyschoffen.mbt.alpha.core.git
  (:require
    [clojure.spec.alpha :as s]
    [clojure.core.protocols :as cp]
    [clojure.datafy :as datafy]
    [cognitect.anomalies :as anom]
    [clj-jgit.porcelain :as git-porcelain]
    [clj-jgit.internal :as git-i]
    [fr.jeremyschoffen.java.nio.alpha.file :as fs]
    [fr.jeremyschoffen.java.nio.alpha.internal.coercions :as coercions]
    [fr.jeremyschoffen.mbt.alpha.core.specs :as specs]
    [fr.jeremyschoffen.mbt.alpha.utils :as u])
  (:import
    (org.eclipse.jgit.revwalk RevTag RevCommit)
    (org.eclipse.jgit.lib Ref PersonIdent)
    (org.eclipse.jgit.api Status Git)
    (org.eclipse.jgit.api.errors RefAlreadyExistsException JGitInternalException)))


(u/pseudo-nss
  git
  git.add!
  git.commit
  git.describe
  git.identity
  git.repo
  git.tag
  project)


(defn- get-dir [^Git repo]
  (let [dir (-> repo
                .getRepository
                .getDirectory)]
    (fs/canonical-path
      (if (-> dir fs/file-name (= (fs/path ".git")))
        (fs/parent dir)
        dir))))

(extend-protocol coercions/UnaryPathBuilder
  Git
  (-to-u-path [this] (get-dir this)))


(defprotocol DatafyJGit
  (datafy-jgit* [x]))

(defn- datafy-jgit-obj [o]
  (vary-meta (datafy-jgit* o) assoc :jgit/object o))


(extend-protocol DatafyJGit
  PersonIdent
  (datafy-jgit* [this] {::git.identity/name (.getName this)
                        ::git.identity/email (.getEmailAddress this)
                        :date (.getWhen this)
                        :time (.getTimeZone this)})
  RevTag
  (datafy-jgit* [this]
    (let [commit (.getObject this)]
      {::git.commit/name (.getName ^RevCommit commit)
       ::git.tag/name (.getTagName this)
       ::git.tag/message (.getFullMessage this)
       ::git.tag/tagger (datafy-jgit-obj (.getTaggerIdent this))}))

  RevCommit
  (datafy-jgit* [this] {::git.commit/name (.getName this)
                        ::git.commit/message (.getFullMessage this)
                        ::git.commit/committer (datafy-jgit-obj (.getCommitterIdent this))
                        ::git.commit/author (datafy-jgit-obj (.getAuthorIdent this))}))

;;----------------------------------------------------------------------------------------------------------------------
;; Simulate git rev-parse
;;----------------------------------------------------------------------------------------------------------------------
(defn- parent-dirs [p]
  (take-while identity (iterate fs/parent (u/safer-path p))))


(defn- parent-repos [p]
  (sequence (comp
              (map #(u/safer-path % ".git"))
              (filter #(fs/exists? %))
              (map fs/parent))
            (parent-dirs p)))


(defn top-level
  "Given the project's working dir, find the git top level.

  Similar to using:
  ```bash
  git -C wd/ rev-parse --show-toplevel
  ```
  "
  [{wd ::project/working-dir}]
  (first (parent-repos wd)))

(u/spec-op top-level
           :param {:req [::project/working-dir]}
           :ret ::git/top-level)


(defn prefix
  "Given the project's working dir, find the git prefix.

  Similar to using:
  ```bash
  git -C wd/ rev-parse --show-prefix
  ```
  "
  [{wd ::project/working-dir :as context}]
  (let [repo (top-level context)]
    (if (= wd repo)
      (fs/path "")
      (fs/relativize repo wd))))

(u/spec-op prefix
           :deps [top-level]
           :param {:req [::project/working-dir]}
           :ret ::git/prefix)


;;----------------------------------------------------------------------------------------------------------------------
;; Repo cstr
;;----------------------------------------------------------------------------------------------------------------------
(defn make-jgit-repo
  "Builds a `org.eclipse.jgit.api.Git` object."
  [param]
  (-> param top-level str git-porcelain/load-repo))

(u/spec-op make-jgit-repo
           :deps [top-level]
           :param {:req [::project/working-dir]}
           :ret ::git/repo)


;;----------------------------------------------------------------------------------------------------------------------
;; git status
;;----------------------------------------------------------------------------------------------------------------------
(defn status
  "Get the status of the current repo via [[clj-jgit.porcelain/git-status]]."
  [{repo ::git/repo}]
  (git-porcelain/git-status repo))

(u/spec-op status
           :param {:req [::git/repo]}
           :ret map?)


;;----------------------------------------------------------------------------------------------------------------------
;; git add
;;----------------------------------------------------------------------------------------------------------------------
(defn- extract-clj-jgit-opts
  "Format an options map used in mbt into a sequence of  k v options used in clj-jgit api.
  Removes the parts that aren't actually options in the clj-jgit api."
  [m main-params]
  (-> (apply dissoc m main-params)
      u/strip-keys-nss
      (->> (apply concat))))


(defn add!
  "Git add operation using [[clj-jgit.porcelain/git-add]]. The parameter and options are specified under the key
  `::fr...mbt.alpha.git/add!`."
  [{repo     ::git/repo
    addition ::git/add!}]
  (let [{patterns ::git.add!/file-patterns} addition
        opts (extract-clj-jgit-opts addition #{::git.add!/file-patterns})]
    (apply git-porcelain/git-add repo patterns opts)))

(u/spec-op add!
           :param {:req [::git/repo
                         ::git/add!]})


(defn list-all-changed-patterns
  "Make a list of strings taken from the `:modified` and `:untracked` sections of a git status.

  See: [[status]]"
  [param]
  (-> param
      status
      (select-keys #{:modified :untracked})
      vals
      (->> (apply concat))))

(u/spec-op list-all-changed-patterns
           :deps [status]
           :param {:req [::git/repo]})


(defn add-all!
  "Use the git add operation on all patterns listed by
  [[fr.jeremyschoffen.mbt.alpha.core.git/list-all-changed-patterns]]. In effect stages modified and un-tracked files."
  [param]
  (let [patterns (list-all-changed-patterns param)]
    (add! (assoc param
            ::git/add! {::git.add!/file-patterns patterns}))))

(u/spec-op add-all!
           :deps [status]
           :param {:req [::git/repo]})


(defn update-all!
  "Similar to [[fr.jeremyschoffen.mbt.alpha.core.git/add-all!]] but uses the `:fr...mbt.alpha.git.add!/update?`
  option set to `true`."
  [param]
  (let [patterns (list-all-changed-patterns param)]
    (add! (assoc param
            ::git/add! {::git.add!/file-patterns patterns
                        ::git.add!/update? true}))))

(u/spec-op update-all!
           :deps [status]
           :param {:req [::git/repo]})


;;----------------------------------------------------------------------------------------------------------------------
;; git commit
;;----------------------------------------------------------------------------------------------------------------------
(defn- commit->commit-opts [commit]
  (-> commit
      (cond-> (contains? commit ::git.commit/author)
              (update ::git.commit/author u/strip-keys-nss)

              (contains? commit ::git.commit/committer)
              (update ::git.commit/committer u/strip-keys-nss))

      (extract-clj-jgit-opts #{::git.commit/message})))


(defn commit!
  "Commit to a git repo using [[clj-jgit.porcelain/git-commit]].
  The options to the porcelain function are passed in the git/commit map,
  see the `:fr...mbt.alpha.git/commit!` spec."
  {:tag RevCommit}
  [{repo ::git/repo
    commit ::git/commit!}]
  (let [{message ::git.commit/message} commit
        opts (commit->commit-opts commit)]
    (datafy-jgit-obj (apply git-porcelain/git-commit repo message opts))))

(u/spec-op commit!
           :param {:req [::git/repo
                         ::git/commit!]}
           :ret ::git/commit)


(defn last-commit
  "Returns the last commit made on the current branch, the first commit to pop up in
  the git log."
  [{repo ::git/repo}]
  (-> (git-porcelain/git-log repo :max-count 1 :jgit? true)
      first
      datafy-jgit-obj))

(u/spec-op last-commit
           :param {:req [::git/repo]}
           :ret ::git/commit)


;;----------------------------------------------------------------------------------------------------------------------
;; git tags
;;----------------------------------------------------------------------------------------------------------------------
(defn- get-tag* [repo id]
  (datafy-jgit-obj
    (with-open [walk (git-i/new-rev-walk repo)]
      (.parseTag walk (git-i/resolve-object id repo)))))


(defn get-tag
  "Get a git tag using its name. The original jgit object is accessible via metadata under the key `:jgit/object`."
  [{repo ::git/repo
    tag-name ::git.tag/name}]
  (get-tag* repo tag-name))

(u/spec-op get-tag
           :deps [get-tag*]
           :param {:req [::git/repo
                         ::git.tag/name]}
           :ret ::git/tag)


(defn- tag->tag-opts [tag]
  (-> tag
      (cond-> (contains? tag ::git.tag/tagger)
              (update ::git.tag/tagger u/strip-keys-nss))

      (extract-clj-jgit-opts #{::git.tag/name})))

(defn- create-tag!*
  {:tag Ref}
  [{repo ::git/repo
    tag ::git/tag!}]
  (let [{name ::git.tag/name} tag
        opts (tag->tag-opts tag)]
    (try
      (apply git-porcelain/git-tag-create repo name opts)

      (catch RefAlreadyExistsException e
        (throw (ex-info (format "The tag %s already exists." name)
                        {::anom/category ::anom/forbidden
                         :mbt/error :tag-already-exists}
                        e)))

      (catch JGitInternalException e
        (throw (ex-info (format "The tag %s already exists." name)
                        {::anom/category ::anom/forbidden
                         :mbt/error :tag-already-exists}
                        e))))))

(u/spec-op create-tag!*
           :param {:req [::git/repo
                         ::git/tag!]}
           :ret #(instance? Ref %))


(defn tag!
  "Create a new git tag using [[clj-jgit.porcelain/git-tag-create]].

  See the `:fr...mbt.alpha.git/tag!` specs for all the options."
  [{repo ::git/repo :as param}]
  (let [tag-ref (create-tag!* param)]
    (get-tag* repo (-> tag-ref .getObjectId))))

(u/spec-op tag!
           :deps [create-tag!*]
           :param {:req [::git/repo
                         ::git/tag!]}
           :ret ::git/tag)

;;----------------------------------------------------------------------------------------------------------------------
;; git describe
;;----------------------------------------------------------------------------------------------------------------------
(defn dirty?
  "Test whether the git repo is dirty or not."
  [{repo ::git/repo}]
  (not (.isClean ^Status (git-porcelain/git-status repo :jgit? true))))

(u/spec-op dirty?
           :param {:req [::git/repo]}
           :ret boolean?)


(defn describe-raw
  "Get a git description.

  Similar to:
  ```
  git describe --long --match tag-pattern
  ```
  `tag-pattern` being the value passed under the key `:fr...mbt.alpha.git.describe/tag-pattern`.

  Can return nil if there are no tag matching the pattern or no tags at all.
  "
  [{repo        ::git/repo
    tag-pattern ::git.describe/tag-pattern}]
  (-> ^Git repo
      .describe
      (.setLong true)
      (cond-> tag-pattern (.setMatch (into-array String [tag-pattern])))
      .call))

(u/spec-op describe-raw
           :param {:req [::git/repo]
                   :opt [::git.describe/tag-pattern]}
           :ret (s/nilable ::git/raw-description))


(def raw-description-regex
  "Regex used to parse a git raw description."
  #"(.*)-(\d+)-g([a-f0-9]*)$")


(defn- parse-description [desc]
  (let [[_ tag distance sha] (re-matches raw-description-regex desc)]
    {::git.tag/name tag
     ::git.describe/distance (Integer/parseInt distance)
     ::git/sha sha}))


(defn describe
  "Get a git description in a map form. This is an evolved version of
  [[fr.jeremyschoffen.mbt.alpha.core.git/describe-raw]] with additional data.

  See the spec `:fr...mbt.alpha.git/description`."
  [{repo ::git/repo
    :as param}]
  (when-let [raw-desc (describe-raw param)]
    (let [{tag-name ::git.tag/name
           :as desc} (parse-description raw-desc)]
      (-> param
          (merge desc)
          (assoc ::git/raw-description raw-desc
                 ::git/tag (get-tag* repo tag-name))
          (u/assoc-computed ::git.repo/dirty? dirty?)
          (select-keys specs/description-keys)))))

(u/spec-op describe
           :deps [describe-raw parse-description get-tag dirty?]
           :param {:req [::git/repo]
                   :opt [::git.describe/tag-pattern]}
           :ret (s/nilable ::git/description))


;;----------------------------------------------------------------------------------------------------------------------
;; Utils
;;----------------------------------------------------------------------------------------------------------------------
(defn any-commit?
  "Test whether the git repo has any commits or not."
  [{repo ::git/repo}]
  (try
    (-> repo git-porcelain/git-log seq)
    (catch Exception _
      false)))

(u/spec-op any-commit?
           :param {:req [::git/repo]}
           :ret boolean?)
