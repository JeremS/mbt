(ns com.jeremyschoffen.mbt.api.git-state-test
  (:require
    [clojure.test :refer [deftest testing]]
    [clojure.spec.alpha :as s]
    [orchestra.spec.test :as st]
    [expound.alpha :as expound]
    [cognitect.anomalies :as anom]
    [testit.core :refer :all]
    [clj-jgit.porcelain :as git]
    [com.jeremyschoffen.mbt.api.helpers_test :as h]
    [com.jeremyschoffen.mbt.api.git :as mbt-git]
    [com.jeremyschoffen.mbt.api.git-state :as gs]
    [com.jeremyschoffen.java.nio.file :as fs]
    [com.jeremyschoffen.mbt.api.utils :as u]))

(st/instrument)
(set! s/*explain-out* expound/printer)

(defn make-project [repo & dirs]
  (let [proj (apply u/safer-path repo dirs)]
    (fs/create-directories! proj)
    (h/copy-dummy-deps proj)
    (fs/relativize repo proj)))


(defn test-simple-usage [repo project-path state]
  (let [untracked-path (->> repo
                            git/git-status
                            :untracked
                            first
                            (u/safer-path repo)
                            str)
        expected-deps-path (str (u/safer-path repo project-path "deps.edn"))]

    (fact
      untracked-path => expected-deps-path)

    (testing "Not tagged yet."
      (facts
        (gs/current-version state) =throws=> (ex-info? identity {::anom/category ::anom/not-found
                                                                 :mbt/error :no-description})
        (gs/create-new-tag! state) =throws=> (ex-info? identity {::anom/category ::anom/not-found
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
        (gs/create-new-tag! state) =throws=> (ex-info? identity {::anom/category ::anom/not-found
                                                                 :mbt/error :no-description})))

    (gs/create-first-tag! state)
    (facts
      (str (gs/current-version state)) => "0"

      (gs/create-first-tag! state) =throws=> (ex-info? identity {::anom/category ::anom/forbidden
                                                                 :mbt/error :already-tagged})
      (gs/create-new-tag! state) =throws=> (ex-info? identity {::anom/category ::anom/forbidden
                                                               :mbt/error :tag-already-exists}))

    (h/add-src repo project-path)
    (h/add-all! repo)
    (git/git-commit repo "commit 2")

    (h/add-src repo project-path)
    (h/add-all! repo)
    (git/git-commit repo "commit 3")

    (gs/create-new-tag! state)
    (testing "after 2 commits, version is 2"
      (fact (str (gs/current-version state)) => "2"))))

(deftest test-state
  (let [simple-repo (h/make-temp-repo)
        simple-project (make-project simple-repo)
        simple-state (gs/get-state {:project/working-dir (u/safer-path simple-repo simple-project)})

        monorepo (h/make-temp-repo)
        project1 (make-project monorepo "container1" "project1")
        project1-state (gs/get-state {:project/working-dir (u/safer-path monorepo project1)})]
    (facts
      (-> simple-repo
          u/safer-path
          fs/file-name
          str)
      => (:artefact/name simple-state)

      (-> monorepo
          u/safer-path
          fs/file-name
          (str "-" "container1" "-" "project1"))
      => (:artefact/name project1-state))

    (test-simple-usage simple-repo simple-project simple-state)
    (test-simple-usage monorepo project1 project1-state)))


(comment
  (def simple-repo (h/make-temp-repo))
  (def simple-project (make-project simple-repo))

  (->> simple-repo
       git/git-status
       :untracked
       first
       (u/safer-path simple-repo))

  (u/safer-path simple-repo simple-project "deps.edn"))


(defn test-versioning [repo project])

(clojure.test/run-tests)
(comment
  (def repo (h/make-temp-repo))
  (def repo2 (h/make-temp-repo))




  (def simple-projet (make-project repo2))
  (def state (gs/get-state {:project/working-dir (u/safer-path repo simple-projet)}))


  (def project1 (make-project repo "container1" "project1"))
  (def project2 (make-project repo "container1" "project2"))

  (h/add-src repo project1 "1")
  (h/add-src repo project2 "1")
  (git/git-status repo)

  (def state1 (gs/get-state {:project/working-dir (u/safer-path repo project1)}))
  (def state2 (gs/get-state {:project/working-dir (u/safer-path repo project2)}))

  (h/add-all! repo)
  (git/git-commit repo  "A commit")
  (gs/create-first-tag! state1)
  (gs/create-new-tag! state1)
  (str (gs/current-version state1))
  (clojure.repl/doc fs/walk))