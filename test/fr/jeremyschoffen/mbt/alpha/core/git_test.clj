(ns fr.jeremyschoffen.mbt.alpha.core.git-test
  (:require
    [clojure.test :refer [deftest testing]]
    [clojure.spec.test.alpha :as st]
    [cognitect.anomalies :as anom]
    [testit.core :refer :all]
    [fr.jeremyschoffen.java.nio.alpha.file :as fs]
    [fr.jeremyschoffen.mbt.alpha.test.helpers :as h]
    [fr.jeremyschoffen.mbt.alpha.core.git :as git]))

(st/instrument [git/any-commit?
                git/commit!
                git/describe
                git/dirty?
                git/get-tag
                git/make-jgit-repo
                git/prefix
                git/status
                git/tag!
                git/top-level
                git/update-all!])


(defn make-temp-repo! []
  (let [repo (h/make-temp-repo!)
        project-name "project"
        project-path (fs/path repo project-name)
        ctxt {:project/working-dir project-path
              :git/repo            repo}]
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
        (git/top-level {:project/working-dir (fs/path repo)}) => (fs/path repo)
        (git/top-level {:project/working-dir project-path}) => (fs/path repo)))

    (testing "Prefix work"
      (facts
        (git/prefix {:project/working-dir (fs/path repo)}) => (fs/path "")
        (git/prefix {:project/working-dir project-path}) => (fs/path project-name)))))


(deftest make-jgit-repo
  (let [{:keys [repo
                project-path]} (make-temp-repo!)]
    (fact (fs/path repo)
          =>(fs/path (git/make-jgit-repo {:project/working-dir project-path})))))


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

        status-at-creation (git/status ctxt)

        file1 (h/add-src! repo project-name "src")
        status-after-creating-a-file (git/status ctxt)

        _ (git/add-all! ctxt)
        status-after-git-add (git/status ctxt)

        _ (git/commit! (assoc ctxt
                         :git/commit! {:git.commit/message "Committed 1 file."}))
        status-after-commit1 (git/status ctxt)

        _ (spit file1 "some text")
        file2 (h/add-src! repo project-name "src")

        status-after-create+modified (git/status ctxt)

        _ (git/add-all! ctxt)
        status-after-second-git-add (git/status ctxt)]

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
        _ (git/add-all! ctxt)
        _ (git/commit! (assoc ctxt :git/commit! {:git.commit/message "commit 1"}))
        file2 (h/add-src! repo project-name "src")
        _ (spit file1 "some content")

        status-after-create+modified (git/status ctxt)
        _ (git/update-all! ctxt)
        status-after-git-add-update (git/status ctxt)]
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
    (git/add-all! ctxt)
    (git/commit! (assoc ctxt
                   :git/commit! {:git.commit/message "A super duper commit."
                                 :git.commit/author {:git.identity/name author-name
                                                     :git.identity/email author-email}
                                 :git.commit/committer {:git.identity/name committer-name
                                                        :git.identity/email committer-email}}))

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

        tag! {:git.tag/name    tag-name
              :git.tag/message tag-msg
              :git.tag/tagger  {:git.identity/name  tagger-name
                                :git.identity/email tagger-email}}

        _    (git/tag! (assoc ctxt :git/tag! tag!))
        tag' (git/get-tag (assoc ctxt :git.tag/name tag-name))]

    (facts
      tag' =in=> tag!

      (git/tag! (assoc ctxt :git/tag! tag!))
      =throws=> (ex-info? "The tag tag already exists."
                          {::anom/category ::anom/forbidden,
                           :mbt/error :tag-already-exists}))))



(deftest dirty?
  (let [{:keys [ctxt]} (make-temp-repo!)
        wd (:project/working-dir ctxt)]

    (testing "At cration the repo is clean"
      (fact (git/dirty? ctxt) => false))


    (testing "After creating file the repo is dirty"
      (h/add-src! wd "src")
      (fact (git/dirty? ctxt) => true))

    (testing "After git add, repos is still dirty."
      (git/add-all! ctxt)
      (fact (git/dirty? ctxt) => true))

    (testing "After committing repo is clean again."
      (git/commit! (assoc ctxt
                     :git/commit! {:git.commit/message "Added 1 file."}))
      (fact (git/dirty? ctxt) => false))))


(deftest describe
  (let [{:keys [ctxt]} (make-temp-repo!)
        wd (:project/working-dir ctxt)]

    (git/tag! (assoc ctxt
                :git/tag! {:git.tag/name "tag0"
                           :git.tag/message "tag0"}))
    (fact
      (git/describe ctxt) =in=> {:git.describe/distance 0
                                 :git/tag {:git.tag/name "tag0"}
                                 :git.repo/dirty? false})

    (h/add-src! wd "src")
    (git/add-all! ctxt)
    (git/commit! (assoc ctxt
                   :git/commit! {:git.commit/message "commit 1"}))

    (git/tag! (assoc ctxt
                :git/tag! {:git.tag/name "tag1"
                           :git.tag/message "tag1"}))

    (facts
      (git/describe ctxt) =in=> {:git.describe/distance 0
                                 :git/tag {:git.tag/name "tag1"}
                                 :git.repo/dirty? false}

      (git/describe (assoc ctxt :git.describe/tag-pattern "tag0"))
      =in=> {:git.describe/distance 1
             :git/tag {:git.tag/name "tag0"}
             :git.repo/dirty? false})

    (h/add-src! wd "src")
    (git/add-all! ctxt)
    (git/commit! (assoc ctxt
                   :git/commit! {:git.commit/message "commit 2"}))

    (h/add-src! wd "src")
    (facts
      (git/describe ctxt) =in=> {:git.describe/distance 1
                                 :git/tag {:git.tag/name "tag1"}
                                 :git.repo/dirty? true}

      (git/describe (assoc ctxt :git.describe/tag-pattern "tag0"))
      =in=> {:git.describe/distance 2
             :git/tag {:git.tag/name "tag0"}
             :git.repo/dirty? true})))


(deftest any-commit?
  (let [temp-dir (fs/create-temp-directory! "temp_repo")
        repo (clj-jgit.porcelain/git-init :dir temp-dir)]
    (fact
      (git/any-commit? {:git/repo repo}) => false)))