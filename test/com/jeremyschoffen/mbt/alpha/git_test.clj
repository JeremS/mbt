(ns com.jeremyschoffen.mbt.alpha.git-test
  (:require
    [clojure.test :refer [deftest testing]]
    [clojure.spec.test.alpha :as st]
    [testit.core :refer :all]
    [com.jeremyschoffen.java.nio.file :as fs]
    [com.jeremyschoffen.mbt.alpha.core.helpers_test :as h]

    [com.jeremyschoffen.mbt.alpha.core.git2 :as git]
    [com.jeremyschoffen.mbt.alpha.core.utils :as u]))

(st/instrument)

(deftest rev-parse-like
  (let [repo (h/make-temp-repo!)
        project-name "project"
        project-path (fs/path repo project-name)]
    (testing "Top level works"
      (facts
        (git/top-level {:project/working-dir (fs/path repo)}) => (fs/path repo)
        (git/top-level {:project/working-dir project-path}) => (fs/path repo)))

    (testing "Prefix work"
      (facts
        (git/prefix {:project/working-dir (fs/path repo)}) => (fs/path "")
        (git/prefix {:project/working-dir project-path}) => (fs/path project-name)))))


(deftest make-jgit-repo
  (let [repo (h/make-temp-repo!)
        project-name "project"
        project-path (fs/path repo project-name)]
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
  (let [repo (h/make-temp-repo!)
        project-name "project"
        project-path (fs/path repo project-name)
        ctxt {:project/working-dir project-path
              :git/repo            repo}

        status-at-creation (git/status ctxt)

        file1 (h/add-src repo project-name "src")
        status-after-creating-a-file (git/status ctxt)

        _ (git/add-all! ctxt)
        status-after-git-add (git/status ctxt)

        _ (git/commit! (assoc ctxt
                         :git/commit {:git.commit/message "Committed 1 file."}))
        status-after-commit1 (git/status ctxt)

        _ (spit file1 "some text")
        file2 (h/add-src repo project-name "src")

        status-after-create+modified (git/status ctxt)

        _ (git/add-all! ctxt)
        status-after-seconde-git-add (git/status ctxt)]

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

      status-after-seconde-git-add => (assoc empty-status
                                        :added #{(file-pattern repo file2)}
                                        :changed #{(file-pattern repo file1)}))))

(comment
  (do
    (def repo (h/make-temp-repo!))
    (def project-name "project")
    (def project-path (fs/path repo project-name))
    (def ctxt {:project/working-dir project-path
               :git/repo            repo}))

  (git/status ctxt)

  (do
    (def added-file1 (h/add-src repo project-name "src"))
    (def added-file2 (h/add-src repo project-name "src")))


  (git/status ctxt)

  (def add-res (git/add! (assoc ctxt
                           :git/addition {:git.addition/file-patterns [(file-pattern repo added-file1)
                                                                       (file-pattern repo added-file2)]})))

  (def add-all-res (git/add-all! ctxt))
  (bean add-all-res)

  (git/status ctxt))