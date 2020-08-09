(ns fr.jeremyschoffen.mbt.alpha.test.helpers_test
  (:require
    [clojure.test :refer [deftest testing]]
    [testit.core :refer :all]
    [clj-jgit.porcelain :as git-p]
    [fr.jeremyschoffen.mbt.alpha.core.git]
    [fr.jeremyschoffen.mbt.alpha.test.helpers :as h]))



(deftest create-repos
  (let [origin (h/make-temp-origin)
        wc (h/make-clone origin)]

    (git-p/git-commit wc "Initial commit.")
    (git-p/git-push wc)
    (facts
      (count (git-p/git-log wc)) => 1
      (count (git-p/git-log origin)) => 1)

    (h/add-src! wc "src")
    (fact (-> wc
              git-p/git-status
              :untracked
              count)
          => 1)

    (h/add-src! wc "src")
    (h/add-all! wc)
    (let [{:keys [untracked added]} (git-p/git-status wc)]
      (facts (count untracked) => 0
             (count added) => 2))

    (let [msg "Added 2 files."]
      (git-p/git-commit wc msg)
      (git-p/git-push wc)

      (let [status (git-p/git-status wc)]
        (facts
          (->> status vals (every? empty?)) => true

          (count (git-p/git-log wc)) => 2
          (count (git-p/git-log origin)) => 2

          (-> origin
              git-p/git-log
              first
              :msg) => msg)))))