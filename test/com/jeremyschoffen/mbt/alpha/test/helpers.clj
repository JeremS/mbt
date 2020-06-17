(ns com.jeremyschoffen.mbt.alpha.test.helpers
  (:require
    [clj-jgit.porcelain :as git-p]
    [com.jeremyschoffen.java.nio.file :as fs]
    [com.jeremyschoffen.mbt.alpha.core.git]
    [com.jeremyschoffen.mbt.alpha.core.utils :as u]))


(defn add-all! [repo]
  (let [status (git-p/git-status repo)]
    (git-p/git-add repo (seq (:untracked status)))))


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


(defn add-src! [repo & dirs]
  (let [temp-dir (fs/path repo)
        path (apply u/safer-path temp-dir dirs)
        _ (assert (fs/ancestor? temp-dir path))
        dest-dir (fs/create-directories! path)]
    (fs/create-temp-file! "temp-src" ".clj" {:dir dest-dir})))