(ns com.jeremyschoffen.mbt.alpha.core.git
  (:require
    [clojure.spec.alpha :as s]
    [clojure.core.protocols :as cp]
    [clojure.datafy :refer [datafy]]
    [cognitect.anomalies :as anom]
    [clj-jgit.porcelain :as git]
    [clj-jgit.internal :as git-i]
    [com.jeremyschoffen.java.nio.file :as fs]
    [com.jeremyschoffen.mbt.alpha.core.specs :as specs]
    [com.jeremyschoffen.mbt.alpha.core.utils :as u])
  (:import
    (org.eclipse.jgit.revwalk RevTag)
    (org.eclipse.jgit.lib Ref)
    (org.eclipse.jgit.api Status Git)
    (org.eclipse.jgit.api.errors RefAlreadyExistsException JGitInternalException)))

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
    (when-not (= wd repo)
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
;; git tags
;;----------------------------------------------------------------------------------------------------------------------
(defn- create-tag!*
  {:tag Ref}
  [{:git/keys [repo]
    :git.tag/keys [name message sign?]}]
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
                      e)))))
;; TODO: could be (s/merge :git/tag (s/keys :req [])
(u/spec-op create-tag!*
           :param {:req [:git/repo :git.tag/name :git.tag/message]
                   :opt [:git.tag/sign?]}
           :ret #(isa? % Ref))


(extend-protocol cp/Datafiable
  RevTag
  (datafy [this] {:git.tag/name (.getTagName this)
                  :git.tag/message (.getFullMessage this)}))


(defn- get-tag* [repo id]
  (datafy
    (with-open [walk (git-i/new-rev-walk repo)]
      (.parseTag walk (git-i/resolve-object id repo)))))


(defn create-tag! [{repo :git/repo :as param}]
  (let [tag-ref (create-tag!* param)]
    (get-tag* repo (-> tag-ref .getObjectId))))

(u/spec-op create-tag!
           :deps [create-tag!*]
           :param {:req [:git/repo :git.tag/name :git.tag/message]
                   :opt [:git.tag/sign?]}
           :ret :git/tag)


(defn get-tag [{repo :git/repo
                tag-name :git.tag/name}]
  (get-tag* repo tag-name))

(u/spec-op get-tag
           :deps [get-tag*]
           :param {:req [:git/repo :git.tag/name]}
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


(defn describe [param]
  (when-let [desc (describe-raw param)]
    (-> param
        (assoc :git/raw-description desc)
        (merge (parse-description desc))
        (u/merge-computed get-tag)
        (u/assoc-computed :git.repo/dirty? dirty?)
        (select-keys specs/description-keys))))

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
