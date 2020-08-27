(ns fr.jeremyschoffen.mbt.alpha.core.git-test
  (:require
    [clojure.test :refer [deftest testing]]
    [clojure.spec.test.alpha :as st]
    [cognitect.anomalies :as anom]
    [testit.core :refer :all]
    [fr.jeremyschoffen.java.nio.alpha.file :as fs]
    [fr.jeremyschoffen.mbt.alpha.test.helpers :as h]
    [fr.jeremyschoffen.mbt.alpha.core.git :as core-git]
    [fr.jeremyschoffen.mbt.alpha.utils :as u]))


(u/pseudo-nss
  git
  git.commit
  git.describe
  git.identity
  git.repo
  git.tag
  project)


(st/instrument `[core-git/any-commit?
                 core-git/commit!
                 core-git/describe
                 core-git/dirty?
                 core-git/get-tag
                 core-git/make-jgit-repo
                 core-git/prefix
                 core-git/status
                 core-git/tag!
                 core-git/top-level
                 core-git/update-all!])


(defn make-temp-repo! []
  (let [repo (h/make-temp-repo!)
        project-name "project"
        project-path (fs/path repo project-name)
        ctxt {::project/working-dir project-path
              ::git/repo            repo}]
    {:repo repo
     :project-name project-name
     :project-path project-path
     :ctxt ctxt}))


(deftest rev-parse-like
  (let [{:keys [repo
                project-name
                project-path ]} (make-temp-repo!)]
    (testing "Top level works"
      (facts
        (core-git/top-level {::project/working-dir (fs/path repo)}) => (fs/path repo)
        (core-git/top-level {::project/working-dir project-path}) => (fs/path repo)))

    (testing "Prefix work"
      (facts
        (core-git/prefix {::project/working-dir (fs/path repo)}) => (fs/path "")
        (core-git/prefix {::project/working-dir project-path}) => (fs/path project-name)))))


(deftest make-jgit-repo
  (let [{:keys [repo
                project-path]} (make-temp-repo!)]
    (fact (fs/path repo)
          => (fs/path (core-git/make-jgit-repo {::project/working-dir project-path})))))


(def empty-status {:added #{}
                   :changed #{}
                   :missing #{}
                   :modified #{}
                   :removed #{}
                   :untracked #{}})


(defn- file-pattern [repo path]
  (->> path
       (fs/relativize repo)
       str))


(deftest basic-git-usage
  (let [{:keys [repo
                project-name
                ctxt]} (make-temp-repo!)

        status-at-creation (core-git/status ctxt)

        file1 (h/add-src! repo project-name "src")
        status-after-creating-a-file (core-git/status ctxt)

        _ (core-git/add-all! ctxt)
        status-after-git-add (core-git/status ctxt)

        _ (core-git/commit! (assoc ctxt
                              ::git/commit! {::git.commit/message "Committed 1 file."}))
        status-after-commit1 (core-git/status ctxt)

        _ (spit file1 "some text")
        file2 (h/add-src! repo project-name "src")

        status-after-create+modified (core-git/status ctxt)

        _ (core-git/add-all! ctxt)
        status-after-second-git-add (core-git/status ctxt)]

    (facts
      status-at-creation => empty-status

      status-after-creating-a-file => (assoc empty-status
                                        :untracked #{(file-pattern repo file1)})

      status-after-git-add => (assoc empty-status
                                :added #{(->> file1
                                              (fs/relativize repo)
                                              str)})

      status-after-commit1 => empty-status

      status-after-create+modified => (assoc empty-status
                                        :untracked #{(file-pattern repo file2)}
                                        :modified #{(file-pattern repo file1)})

      status-after-second-git-add => (assoc empty-status
                                        :added #{(file-pattern repo file2)}
                                        :changed #{(file-pattern repo file1)}))))

(deftest add
  (let [{:keys [repo
                project-name
                ctxt ]} (make-temp-repo!)

        file1 (h/add-src! repo project-name "src")
        _ (core-git/add-all! ctxt)
        _ (core-git/commit! (assoc ctxt ::git/commit! {::git.commit/message "commit 1"}))
        file2 (h/add-src! repo project-name "src")
        _ (spit file1 "some content")

        status-after-create+modified (core-git/status ctxt)
        _ (core-git/update-all! ctxt)
        status-after-git-add-update (core-git/status ctxt)]
    (facts
      status-after-create+modified => (assoc empty-status
                                        :untracked #{(file-pattern repo file2)}
                                        :modified #{(file-pattern repo file1)})

      status-after-git-add-update => (assoc empty-status
                                       :untracked #{(file-pattern repo file2)}
                                       :changed #{(file-pattern repo file1)}))))


(deftest commit
  (let [{:keys [repo
                project-name
                ctxt ]} (make-temp-repo!)

        author-name "tester"
        author-email "tester@test.com"

        committer-name "admin"
        committer-email "admin@admin.com"]
    (h/add-src! repo project-name "src")
    (h/add-src! repo project-name "src")
    (core-git/add-all! ctxt)
    (core-git/commit! (assoc ctxt
                        ::git/commit! {::git.commit/message "A super duper commit."
                                       ::git.commit/author {::git.identity/name author-name
                                                            ::git.identity/email author-email}
                                       ::git.commit/committer {::git.identity/name committer-name
                                                               ::git.identity/email committer-email}}))

    (let [last-commit (first (clj-jgit.porcelain/git-log repo))
          {:keys [author committer]} last-commit]

      (facts
        (:name author)  => author-name
        (:email author) => author-email

        (:name committer)  => committer-name
        (:email committer) => committer-email))))


(deftest tag
  (let [{:keys [ctxt]} (make-temp-repo!)

        tag-name "tag"
        tag-msg "A message!"
        tagger-name "tester"
        tagger-email "tester@test.com"

        tag! {::git.tag/name    tag-name
              ::git.tag/message tag-msg
              ::git.tag/tagger  {::git.identity/name  tagger-name
                                 ::git.identity/email tagger-email}}

        _    (core-git/tag! (assoc ctxt ::git/tag! tag!))
        tag' (core-git/get-tag (assoc ctxt ::git.tag/name tag-name))]

    (facts
      tag' =in=> tag!

      (core-git/tag! (assoc ctxt ::git/tag! tag!))
      =throws=> (ex-info? "The tag tag already exists."
                          {::anom/category ::anom/forbidden,
                           :mbt/error :tag-already-exists}))))



(deftest dirty?
  (let [{:keys [ctxt]} (make-temp-repo!)
        wd (::project/working-dir ctxt)]

    (testing "At cration the repo is clean"
      (fact (core-git/dirty? ctxt) => false))


    (testing "After creating file the repo is dirty"
      (h/add-src! wd "src")
      (fact (core-git/dirty? ctxt) => true))

    (testing "After git add, repos is still dirty."
      (core-git/add-all! ctxt)
      (fact (core-git/dirty? ctxt) => true))

    (testing "After committing repo is clean again."
      (core-git/commit! (assoc ctxt
                          ::git/commit! {::git.commit/message "Added 1 file."}))
      (fact (core-git/dirty? ctxt) => false))))


(deftest describe
  (let [{:keys [ctxt]} (make-temp-repo!)
        wd (::project/working-dir ctxt)]

    (core-git/tag! (assoc ctxt
                     ::git/tag! {::git.tag/name "tag0"
                                 ::git.tag/message "tag0"}))
    (fact
      (core-git/describe ctxt) =in=> {::git.describe/distance 0
                                      ::git/tag {::git.tag/name "tag0"}
                                      ::git.repo/dirty? false})

    (h/add-src! wd "src")
    (core-git/add-all! ctxt)
    (core-git/commit! (assoc ctxt
                        ::git/commit! {::git.commit/message "commit 1"}))

    (core-git/tag! (assoc ctxt
                     ::git/tag! {::git.tag/name "tag1"
                                 ::git.tag/message "tag1"}))

    (facts
      (core-git/describe ctxt) =in=> {::git.describe/distance 0
                                      ::git/tag                    {::git.tag/name "tag1"}
                                      ::git.repo/dirty?            false}

      (core-git/describe (assoc ctxt ::git.describe/tag-pattern "tag0"))
      =in=> {::git.describe/distance 1
             ::git/tag {::git.tag/name "tag0"}
             ::git.repo/dirty? false})

    (h/add-src! wd "src")
    (core-git/add-all! ctxt)
    (core-git/commit! (assoc ctxt
                        ::git/commit! {::git.commit/message "commit 2"}))

    (h/add-src! wd "src")
    (facts
      (core-git/describe ctxt) =in=> {::git.describe/distance 1
                                      ::git/tag                    {::git.tag/name "tag1"}
                                      ::git.repo/dirty?            true}

      (core-git/describe (assoc ctxt ::git.describe/tag-pattern "tag0"))
      =in=> {::git.describe/distance 2
             ::git/tag {::git.tag/name "tag0"}
             ::git.repo/dirty? true})))


(deftest any-commit?
  (let [temp-dir (fs/create-temp-directory! "temp_repo")
        repo (clj-jgit.porcelain/git-init :dir temp-dir)]
    (fact
      (core-git/any-commit? {::git/repo repo}) => false)))
