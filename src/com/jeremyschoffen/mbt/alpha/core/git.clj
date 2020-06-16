(ns com.jeremyschoffen.mbt.alpha.core.git
  (:require
    [clojure.spec.alpha :as s]
    [clojure.core.protocols :as cp]
    [clojure.datafy :as datafy]
    [cognitect.anomalies :as anom]
    [clj-jgit.porcelain :as git]
    [clj-jgit.internal :as git-i]
    [com.jeremyschoffen.java.nio.file :as fs]
    [com.jeremyschoffen.java.nio.internal.coercions :as coercions]
    [com.jeremyschoffen.mbt.alpha.core.specs :as specs]
    [com.jeremyschoffen.mbt.alpha.core.utils :as u])
  (:import
    (org.eclipse.jgit.revwalk RevTag RevCommit)
    (org.eclipse.jgit.lib Ref PersonIdent)
    (org.eclipse.jgit.api Status Git)
    (org.eclipse.jgit.api.errors RefAlreadyExistsException JGitInternalException)))


(defn get-dir [^Git repo]
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


(defn datafy-jgit-obj [o]
  (vary-meta (datafy/datafy o) assoc :jgit/object o))


(extend-protocol cp/Datafiable
  PersonIdent
  (datafy [this] {:git.identity/name (.getName this)
                  :git.identity/email (.getEmailAddress this)
                  :date (.getWhen this)
                  :time (.getTimeZone this)})
  RevTag
  (datafy [this]
    {:git.tag/name (.getTagName this)
     :git.tag/message (.getFullMessage this)
     :git.tag/tagger (datafy-jgit-obj (.getTaggerIdent this))})

  RevCommit
  (datafy [this] {:git.commit/name (.getName this)
                  :git.commit/message (.getFullMessage this)
                  :git.commit/committer (datafy-jgit-obj (.getCommitterIdent this))
                  :git.commit/author (datafy-jgit-obj (.getAuthorIdent this))}))

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
  [{wd :project/working-dir}]
  (first (parent-repos wd)))

(u/spec-op top-level
           :param {:req [:project/working-dir]}
           :ret :git/top-level)


(defn prefix [{wd :project/working-dir :as context}]
  (let [repo (top-level context)]
    (if (= wd repo)
      (fs/path "")
      (fs/relativize repo wd))))

(u/spec-op prefix
           :deps [top-level]
           :param {:req [:project/working-dir]}
           :ret :git/prefix)


;;----------------------------------------------------------------------------------------------------------------------
;; Repo cstr
;;----------------------------------------------------------------------------------------------------------------------
(defn make-jgit-repo [param]
  (-> param top-level str git/load-repo))

(u/spec-op make-jgit-repo
           :deps [top-level]
           :param {:req [:project/working-dir]}
           :ret :git/repo)


;;----------------------------------------------------------------------------------------------------------------------
;; git status
;;----------------------------------------------------------------------------------------------------------------------
(defn status [{repo :git/repo}]
  (git/git-status repo))

(u/spec-op status
           :param {:req [:git/repo]}
           :ret map?)


;;----------------------------------------------------------------------------------------------------------------------
;; git add
;;----------------------------------------------------------------------------------------------------------------------
(defn- format-opts [m keys]
  (-> (apply dissoc m keys)
      u/strip-keys-nss
      seq
      flatten))


(defn add! [{repo     :git/repo
             addition :git/addition}]
  (let [{patterns :git.addition/file-patterns} addition
        opts (format-opts addition #{:git.addition/file-patterns})]
    (apply git/git-add repo patterns opts)))

(u/spec-op add!
           :param {:req [:git/repo
                         :git/addition]})


(defn list-all-changed-patterns [param]
  (-> param
      status
      (select-keys #{:modified :untracked})
      vals
      (->> (apply concat))))

(u/spec-op list-all-changed-patterns
           :deps [status]
           :param {:req [:git/repo]})


(defn add-all! [param]
  (let [patterns (list-all-changed-patterns param)]
    (add! (assoc param
            :git/addition {:git.addition/file-patterns patterns}))))

(u/spec-op add-all!
           :deps [status]
           :param {:req [:git/repo]})


(defn update-all! [param]
  (let [patterns (list-all-changed-patterns param)]
    (add! (assoc param
            :git/addition {:git.addition/file-patterns patterns
                           :git.addition/update? true}))))

(u/spec-op update-all!
           :deps [status]
           :param {:req [:git/repo]})


;;----------------------------------------------------------------------------------------------------------------------
;; git commit
;;----------------------------------------------------------------------------------------------------------------------
(defn- commit->commit-opts [commit]
  (-> commit
      (cond-> (contains? commit :git.commit/author)
              (update :git.commit/author u/strip-keys-nss)

              (contains? commit :git.commit/committer)
              (update :git.commit/committer u/strip-keys-nss))

      (format-opts #{:git.commit/message})))


(defn commit!
  "Commit to a git repo using `clj-jgit.porcelain/git-commit`.
  The options to the porcelain function are passed in the git/commit map,
  se the :git/commit spec."
  {:tag RevCommit}
  [{repo :git/repo
    commit :git/commit!}]
  (let [{message :git.commit/message} commit
        opts (commit->commit-opts commit)]
    (datafy-jgit-obj (apply git/git-commit repo message opts))))

(u/spec-op commit!
           :param {:req [:git/repo :git/commit!]})


;;----------------------------------------------------------------------------------------------------------------------
;; git tags
;;----------------------------------------------------------------------------------------------------------------------
(defn- get-tag* [repo id]
  (datafy-jgit-obj
    (with-open [walk (git-i/new-rev-walk repo)]
      (.parseTag walk (git-i/resolve-object id repo)))))


(defn get-tag [{repo :git/repo
                tag-name :git.tag/name}]
  (get-tag* repo tag-name))

(u/spec-op get-tag
           :deps [get-tag*]
           :param {:req [:git/repo :git.tag/name]}
           :ret :git/tag)


(defn- tag->tag-opts [tag]
  (-> tag
      (cond-> (contains? tag :git.tag/tagger)
              (update :git.tag/tagger u/strip-keys-nss))

      (format-opts #{:git.tag/name})))

(defn- create-tag!*
  {:tag Ref}
  [{repo :git/repo
    tag :git/tag!}]
  (let [{name :git.tag/name} tag
        opts (tag->tag-opts tag)]
    (try
      (apply git/git-tag-create repo name opts)

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
           :param {:req [:git/repo :git/tag!]}
           :ret #(instance? Ref %))


(defn create-tag! [{repo :git/repo :as param}]
  (let [tag-ref (create-tag!* param)]
    (get-tag* repo (-> tag-ref .getObjectId))))

(u/spec-op create-tag!
           :deps [create-tag!*]
           :param {:req [:git/repo :git/tag!]}
           :ret :git/tag)

;;----------------------------------------------------------------------------------------------------------------------
;; git describe
;;----------------------------------------------------------------------------------------------------------------------
(defn dirty? [{repo :git/repo}]
  (not (.isClean ^Status (git/git-status repo :jgit? true))))

(u/spec-op dirty?
           :param {:req [:git/repo]}
           :ret boolean?)


(defn describe-raw [{repo        :git/repo
                     tag-pattern :git.describe/tag-pattern}]
  (-> ^Git repo
      .describe
      (.setLong true)
      (cond-> tag-pattern (.setMatch (into-array String [tag-pattern])))
      .call))

(u/spec-op describe-raw
           :param {:req [:git/repo]
                   :opt [:git.describe/tag-pattern]}
           :ret (s/nilable :git/raw-description))


(def raw-description-regex #"(.*)-(\d+)-g([a-f0-9]*)$")


(defn- parse-description [desc]
  (let [[_ tag distance sha] (re-matches raw-description-regex desc)]
    {:git.tag/name tag
     :git.describe/distance (Integer/parseInt distance)
     :git/sha sha}))


(defn describe [{repo :git/repo
                 :as param}]
  (when-let [raw-desc (describe-raw param)]
    (let [{tag-name :git.tag/name
           :as desc} (parse-description raw-desc)]
      (-> param
          (merge desc)
          (assoc :git/raw-description raw-desc
                 :git/tag (get-tag* repo tag-name))
          (u/assoc-computed :git.repo/dirty? dirty?)
          (select-keys specs/description-keys)))))

(u/spec-op describe
           :deps [describe-raw parse-description get-tag dirty?]
           :param {:req [:git/repo]
                   :opt [:git.describe/tag-pattern]}
           :ret (s/nilable :git/description))


;;----------------------------------------------------------------------------------------------------------------------
;; Utils
;;----------------------------------------------------------------------------------------------------------------------
(defn any-commit? [{repo :git/repo}]
  (-> repo
      git/git-log
      empty?
      not))

(u/spec-op any-commit?
           :param {:req [:git/repo]}
           :ret boolean?)
