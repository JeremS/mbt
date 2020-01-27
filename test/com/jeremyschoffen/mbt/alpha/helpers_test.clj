(ns com.jeremyschoffen.mbt.alpha.helpers_test
  (:require
    [clojure.test :refer [deftest testing]]
    [testit.core :refer :all]
    [clj-jgit.porcelain :as git-p]
    [com.jeremyschoffen.java.nio.internal.coercions :as coercions]
    [com.jeremyschoffen.java.nio.file :as fs]
    [com.jeremyschoffen.mbt.alpha.utils :as u])
  (:import (org.eclipse.jgit.api Git)))

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


(defn add-all! [repo]
  (let [status (git-p/git-status repo)]
    (doseq [path (:untracked status)]
      (git-p/git-add repo path))))

(defn copy-dummy-deps [dest-dir]
  (let [src-dummy-deps (u/safer-path "resources-test" "dummy-deps.edn")
        dest (u/safer-path dest-dir "deps.edn")]
    (fs/copy! src-dummy-deps dest)))



(defn make-temp-repo! []
  (let [temp-dir (fs/create-temp-directory! "temp_repo")
        repo (git-p/git-init :dir temp-dir)]
    (git-p/git-commit repo "Initial commit")
    repo))


(defn make-temp-origin []
  (let [temp-dir (fs/create-temp-directory! "temp_bare_repo")
        repo (git-p/git-init :dir temp-dir :bare? true)]
    repo))


(defn make-clone [origin]
  (let [temp-dir (fs/create-temp-directory! "temp_cloned_repo")]
    (-> origin
        fs/uri
        str
        (git-p/git-clone :dir temp-dir))))


(defn add-src [repo & dirs]
  (let [temp-dir (get-dir repo)
        path (apply u/safer-path temp-dir dirs)
        _ (assert (fs/ancestor? temp-dir path))
        dest-dir (fs/create-directories! path)]
    (fs/create-temp-file! "temp-src" ".clj" :dir dest-dir)))


(deftest create-repos
  (let [origin (make-temp-origin)
        wc (make-clone origin)]

    (git-p/git-commit wc "Initial commit.")
    (git-p/git-push wc)
    (facts
      (count (git-p/git-log wc)) => 1
      (count (git-p/git-log origin)) => 1)

    (add-src wc "src")
    (fact (-> wc
              git-p/git-status
              :untracked
              count)
          => 1)

    (add-src wc "src")
    (add-all! wc)
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