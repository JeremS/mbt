(ns com.jeremyschoffen.mbt.alpha.core.git2
  (:require
    [clojure.spec.alpha :as s]
    [clojure.core.protocols :as cp]
    [clojure.datafy :refer [datafy]]
    [cognitect.anomalies :as anom]
    [clj-jgit.porcelain :as git]
    [clj-jgit.internal :as git-i]
    [com.jeremyschoffen.java.nio.file :as fs]
    [com.jeremyschoffen.java.nio.internal.coercions :as coercions]
    [com.jeremyschoffen.mbt.alpha.core.specs :as specs]
    [com.jeremyschoffen.mbt.alpha.core.utils :as u])
  (:import
    (org.eclipse.jgit.revwalk RevTag RevCommit)
    (org.eclipse.jgit.lib Ref)
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
  (flatten (seq (apply dissoc m keys))))


(defn add! [{repo     :git/repo
             addition :git/addition}]
  (let [{patterns :git.addition/file-patterns} addition
        opts (format-opts addition #{:git.addition/file-patterns})]
    (apply git/git-add repo patterns opts)))

(u/spec-op add!
           :param {:req [:git/repo
                         :git/addition]})


(defn add-all! [param]
  (let [patterns (-> param
                     status
                     (select-keys #{:modified :untracked})
                     vals
                     (->> (apply concat)))]
    (add! (assoc param
            :git/addition {:git.addition/file-patterns patterns}))))

(u/spec-op add-all!
           :deps [status]
           :param {:req [:git/repo]})


;;----------------------------------------------------------------------------------------------------------------------
;; git commit
;;----------------------------------------------------------------------------------------------------------------------
(defn commit!
  "Commit to a git repo using `clj-jgit.porcelain/git-commit`.
  The options to the porcelain function are passed in the git/commit map,
  se the :git/commit spec."
  {:tag RevCommit}
  [{repo :git/repo
    commit :git/commit}]
  (let [{message :git.commit/message} commit
        opts (format-opts commit #{:git.commit/message})]
    (apply git/git-commit repo message opts)))

(u/spec-op commit!
           :param {:req [:git/repo :git/commit]})


;;----------------------------------------------------------------------------------------------------------------------
;; git tags
;;----------------------------------------------------------------------------------------------------------------------
(extend-protocol cp/Datafiable
  RevTag
  (datafy [this] {:git.tag/name (.getTagName this)
                  :git.tag/message (.getFullMessage this)}))


(defn- get-tag* [repo id]
  (datafy
    (with-open [walk (git-i/new-rev-walk repo)]
      (.parseTag walk (git-i/resolve-object id repo)))))


(defn get-tag [{repo :git/repo
                tag-name :git.tag/name}]
  (get-tag* repo tag-name))

(u/spec-op get-tag
           :deps [get-tag*]
           :param {:req [:git/repo :git.tag/name]}
           :ret :git/tag)


(defn- create-tag!*
  {:tag Ref}
  [{repo :git/repo
    tag :git/tag}]
  (let [{:git.tag/keys [name message sign?]} tag]
    (try
      (git/git-tag-create repo
                          name
                          :message message
                          :annotated? true
                          :signed? (boolean sign?))

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
           :param {:req [:git/repo :git/tag]}
           :ret #(instance? Ref %))