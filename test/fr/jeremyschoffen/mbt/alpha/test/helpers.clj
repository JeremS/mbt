(ns fr.jeremyschoffen.mbt.alpha.test.helpers
  (:require
    [clj-jgit.porcelain :as git-p]
    [fr.jeremyschoffen.java.nio.alpha.file :as fs]
    [fr.jeremyschoffen.mbt.alpha.core :as mbt-core]
    [fr.jeremyschoffen.mbt.alpha.utils :as u]))


(defn jar-content [jar-path]
  (with-open [zfs (mbt-core/jar-open-fs jar-path)]
    (->> zfs
         fs/walk
         fs/realize
         (into {}
               (comp
                 (remove fs/directory?)
                 (map #(vector (str %) (slurp %))))))))


(defn add-all! [repo]
  (let [status (git-p/git-status repo)]
    (git-p/git-add repo (seq (:untracked status)))))


(defn copy-dummy-deps [dest-dir]
  (let [src-dummy-deps (u/safer-path "resources-test" "dummy-deps.edn")
        dest (u/safer-path dest-dir "deps.edn")]
    (u/ensure-parent! dest)
    (fs/copy! src-dummy-deps dest)))


(defn make-uncommited-temp-repo! []
  (let [temp-dir (fs/create-temp-directory! "temp_repo")]
    (git-p/git-init :dir temp-dir)))


(defn make-temp-repo! []
  (let [repo (make-uncommited-temp-repo!)]
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