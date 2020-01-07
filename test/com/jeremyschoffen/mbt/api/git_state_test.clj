(ns com.jeremyschoffen.mbt.api.git-state-test
  (:require
    [clojure.test :refer [deftest testing]]
    [clojure.spec.alpha :as s]
    [clojure.spec.test.alpha :as st]
    [expound.alpha :as expound]
    [cognitect.anomalies :as anom]
    [testit.core :refer :all]
    [clj-jgit.porcelain :as git]
    [com.jeremyschoffen.mbt.api.helpers_test :as h]
    [com.jeremyschoffen.mbt.api.git :as mbt-git]
    [com.jeremyschoffen.mbt.api.git-state :as gs]
    [com.jeremyschoffen.java.nio.file :as fs]
    [com.jeremyschoffen.mbt.api.utils :as u]
    [com.jeremyschoffen.mbt.api.version :as version]
    [clojure.test :as test]))


(st/instrument)
(set! s/*explain-out* expound/printer)

(defn make-project! [repo & dirs]
  (let [proj (apply u/safer-path repo dirs)]
    (fs/create-directories! proj)
    (h/copy-dummy-deps proj)
    (fs/relativize repo proj)))


(defn make-test-repo! [version-scheme & dirs]
  (let [repo (h/make-temp-repo!)
        project (apply  make-project! repo dirs)]
    (gs/get-state {:project/working-dir (u/safer-path repo project)
                   :version/scheme      version-scheme})))

(defn test-name [& dirs]
  (let [{repo :git/repo
         n :artefact/name} (apply make-test-repo! version/simple-scheme dirs)
        suffix (->> dirs
                    (interleave (repeat "-"))
                    (apply str))]
    (fact
      (-> repo
          u/safer-path
          fs/file-name
          (str suffix))
      => n)))

(deftest testing-names
  (test-name)
  (test-name "c1" "p2"))


(defn basics [state]
  (let [{repo :git/repo
         wd :project/working-dir} state
        untracked-path (->> repo
                            git/git-status
                            :untracked
                            first
                            (u/safer-path repo)
                            str)
        expected-deps-path (str (u/safer-path wd "deps.edn"))]
    (fact
      untracked-path => expected-deps-path)

    (testing "No tag yet."
      (facts
        (gs/current-version state) =throws=> (ex-info? identity {::anom/category ::anom/not-found
                                                                 :mbt/error :no-description})
        (gs/bump-tag! state) =throws=> (ex-info? identity {::anom/category  ::anom/not-found
                                                           :mbt/error :no-description})))

    (testing "Repo is dirty"
      (facts (gs/create-first-tag! state) =throws=> (ex-info? identity {::anom/category ::anom/forbidden
                                                                        :mbt/error :dirty-repo})))

    (h/add-all! repo)
    (git/git-commit repo "initial commit")

    (testing "Still no tag."
      (facts
        (gs/current-version state) =throws=> (ex-info? identity {::anom/category ::anom/not-found
                                                                 :mbt/error :no-description})
        (gs/bump-tag! state) =throws=> (ex-info? identity {::anom/category  ::anom/not-found
                                                           :mbt/error :no-description})))))

(deftest testing-basics
  (let [simple-repo (make-test-repo! version/simple-scheme)
        monorepo-simple (make-test-repo! version/simple-scheme "container1" "project1")
        monorepo-semver (make-test-repo! version/semver-scheme "container1" "project2")
        monorepo-maven (make-test-repo! version/maven-scheme "container1" "project3")]
    (basics simple-repo)
    (basics monorepo-simple)
    (basics monorepo-semver)
    (basics monorepo-maven)))

(defn simple-versioning [state]
  (let [{repo :git/repo
         wd :project/working-dir} state
        project-path (fs/relativize repo wd)]
    (h/add-all! repo)
    (git/git-commit repo "initial commit")
    (gs/create-first-tag! state)

    (facts
      (str (gs/current-version state)) => "0"

      (gs/create-first-tag! state) =throws=> (ex-info? identity {::anom/category ::anom/forbidden
                                                                 :mbt/error :already-tagged})
      (gs/bump-tag! state) =throws=> (ex-info? identity {::anom/category  ::anom/forbidden
                                                         :mbt/error :tag-already-exists}))

    (h/add-src repo project-path)
    (h/add-all! repo)
    (git/git-commit repo "commit 2")

    (h/add-src repo project-path)
    (h/add-all! repo)
    (git/git-commit repo "commit 3")

    (gs/bump-tag! state)
    (testing "after 2 commits, version is 2"
      (fact (str (gs/current-version state)) => "2"))))


(deftest testing-simple-versioning
  (let [simple-repo (make-test-repo! version/simple-scheme)
        monorepo-simple (make-test-repo! version/simple-scheme "container1" "project1")]
    (simple-versioning simple-repo)
    (simple-versioning monorepo-simple)))


(defn maven-style-versioning2 [state]
  (let [{repo :git/repo
         wd :project/working-dir} state
        project-path (fs/relativize repo wd)]

    (testing "Creating initial tag."
      (h/add-all! repo)
      (git/git-commit repo "Added deps.edn")
      (gs/create-first-tag! state)

      (facts
        (str (gs/current-version state)) => "0.1.0"

        (gs/create-first-tag! state) =throws=> (ex-info? identity {::anom/category ::anom/forbidden
                                                                   :mbt/error :already-tagged})
        (gs/bump-tag! state) =throws=> (ex-info? identity {::anom/category  ::anom/forbidden
                                                           :mbt/error :versioning/duplicating-tag})))


    (testing "Creating first patch after 2 commits."
      (h/add-src repo project-path)
      (h/add-all! repo)
      (git/git-commit repo "commit 2")

      (h/add-src repo project-path)
      (h/add-all! repo)
      (git/git-commit repo "commit 3")

      (gs/bump-tag! state)

      (fact
        (str (gs/current-version state)) => "0.1.1"))

    (testing "Re-patching directly won't work"
      (fact
        (gs/bump-tag! state) =throws=> (ex-info? identity {::anom/category ::anom/forbidden
                                                           :mbt/error :versioning/duplicating-tag})))

    (testing "Upgrading the last patch to a minor version should work"
      (gs/bump-tag! (assoc state :version/bump-level :minor))
      (fact (count (git/git-tag-list repo)) => 3))))



(deftest testing-maven-style-versioning
  (let [monorepo-semver (make-test-repo! version/semver-scheme "container1" "project-semver")
        monorepo-maven (make-test-repo! version/maven-scheme "container1" "project-maven")]
    (maven-style-versioning2 monorepo-semver)
    (maven-style-versioning2 monorepo-maven)))
