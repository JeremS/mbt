(ns com.jeremyschoffen.mbt.alpha.versioning.git-state-test
  (:require
    [clojure.test :refer [deftest testing]]
    [clojure.spec.alpha :as s]
    [clojure.spec.test.alpha :as st]
    [expound.alpha :as expound]
    [cognitect.anomalies :as anom]
    [testit.core :refer :all]
    [clj-jgit.porcelain :as git]
    [com.jeremyschoffen.mbt.alpha.helpers_test :as h]
    [com.jeremyschoffen.mbt.alpha.versioning.git-state :as gs]
    [com.jeremyschoffen.mbt.alpha.classic-scheme :as cs]
    [com.jeremyschoffen.java.nio.file :as fs]
    [com.jeremyschoffen.mbt.alpha.utils :as u]
    [com.jeremyschoffen.mbt.alpha.versioning.schemes :as version]))


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
    (cs/get-state {:project/working-dir (u/safer-path repo project)
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
        (version/current-version state) =throws=> (ex-info? identity {::anom/category ::anom/not-found
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
        (version/current-version state) =throws=> (ex-info? identity {::anom/category ::anom/not-found
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
      (str (version/current-version state)) => "0"

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
      (fact (str (version/current-version state)) => "2"))))


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
        (str (version/current-version state)) => "0.1.0"

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
        (str (version/current-version state)) => "0.1.1"))

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


(deftest mono-repo-versioning
  (let [repo (h/make-temp-repo!)
        p-maven-name "p-maven"
        p-semver-name "p-semver"
        p-simple-name "p-simple"
        p-maven (make-project! repo "c1" p-maven-name)
        p-semver (make-project! repo "c1" p-semver-name)
        p-simple (make-project! repo "c1" p-simple-name)

        state-p-maven (cs/get-state {:project/working-dir (u/safer-path repo p-maven)
                                     :version/scheme      version/maven-scheme})
        state-p-semver (cs/get-state {:project/working-dir (u/safer-path repo p-semver)
                                      :version/scheme      version/semver-scheme})
        state-p-simple (cs/get-state {:project/working-dir (u/safer-path repo p-simple)
                                      :version/scheme      version/simple-scheme})]
    (h/add-all! repo)
    (git/git-commit repo "added p-maven & p-semver & p-simple")
    (git/git-log repo)

    (gs/create-first-tag! state-p-maven)
    (gs/create-first-tag! state-p-semver)
    (gs/create-first-tag! state-p-simple)

    (testing "Every project is at its initial version."
      (facts
        (str (version/current-version state-p-maven)) => "0.1.0"
        (str (version/current-version state-p-semver)) => "0.1.0"
        (str (version/current-version state-p-simple)) => "0"))

    (testing "Added a src to p-maven, comitted and bumped."
      (h/add-src repo "c1" p-maven-name "src")
      (h/add-all! repo)
      (git/git-commit repo "added a src to p1")
      (gs/bump-tag! state-p-maven)

      (facts
        (str (version/current-version state-p-maven)) => "0.1.1"
        (str (version/current-version state-p-semver)) =in=> #"0.1.0-1-0x.*"
        (str (version/current-version state-p-simple)) =in=> #"0.1-0x.*"))


    (testing "Added and comitted a src to p-simple"
      (h/add-src repo "c1" p-simple-name "src")
      (h/add-all! repo)
      (git/git-commit repo "added a src to p-simple")

      (facts
        (str (version/current-version state-p-maven)) =in=> #"0.1.1-1-0x.*"
        (str (version/current-version state-p-semver)) =in=> #"0.1.0-2-0x.*"
        (str (version/current-version state-p-simple)) =in=> #"0.2-0x.*"))

    (testing "Just bumped p-simple"
      (gs/bump-tag! state-p-simple)

      (facts
        (str (version/current-version state-p-maven)) =in=> #"0.1.1-1-0x.*"
        (str (version/current-version state-p-semver)) =in=> #"0.1.0-2-0x.*"
        (str (version/current-version state-p-simple)) =in=> "2"))

    (testing "Just added a src to semver"
      (h/add-src repo "c1" p-semver-name "src")

      (facts
        (str (version/current-version state-p-maven)) =in=> #"0.1.1-1-0x.*-DIRTY$"
        (str (version/current-version state-p-semver)) =in=> #"0.1.0-2-0x.*-DIRTY$"
        (str (version/current-version state-p-simple)) => "2-DIRTY"))

    (testing "Comitted the src to semver and bumped it."
      (h/add-all! repo)
      (git/git-commit repo "added a src to p2")
      (gs/bump-tag! state-p-semver)

      (facts
        (str (version/current-version state-p-maven)) =in=> #"0.1.1-2-0x.*$"
        (str (version/current-version state-p-semver)) =in=> #"0.1.1"
        (str (version/current-version state-p-simple)) =in=> #"2.1-0x.*$"))))
