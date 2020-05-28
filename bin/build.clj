(ns build
  (:require
    [clojure.spec.test.alpha :as spec-test]
    [clojure.string :as string]
    [clj-jgit.porcelain :as jgit]
    [com.jeremyschoffen.java.nio.file :as fs]
    [com.jeremyschoffen.mbt.alpha.classic-scheme :as c]
    [com.jeremyschoffen.mbt.alpha.versioning.schemes :as vs]
    [com.jeremyschoffen.mbt.alpha.versioning.git-state :as gs]
    [com.jeremyschoffen.mbt.alpha.git :as git]
    [com.jeremyschoffen.mbt.alpha.utils :as u]))


(spec-test/instrument)


;;----------------------------------------------------------------------------------------------------------------------
;; utils
;;----------------------------------------------------------------------------------------------------------------------


(def version-file-path (u/safer-path "src" "com" "jeremyschoffen" "mbt" "alpha" "version.clj"))


(defn write-version-file! [ctxt]
  (let [next-version (gs/next-version ctxt)
        updated-next-version (if (= next-version (vs/initial-version ctxt))
                               next-version
                               (update next-version :base-number inc))]
    (spit version-file-path
          (string/join "\n" ["(ns com.jeremyschoffen.mbt.alpha.version)"
                             ""
                             (format "(def version \"%s\")" updated-next-version)
                             ""]))))


(defn git-add-version-file! [{wd :project/working-dir
                              repo :git/repo}]
  (jgit/git-add repo (str (fs/relativize wd version-file-path))))


(defn commit-version-file [{repo :git/repo}]
  (when-not (->> repo
                 jgit/git-status
                 vals
                 (apply concat)
                 empty?))
  (jgit/git-commit repo "Committed version file."))


(defn add-version-file! [ctxt]
  (-> ctxt
      (u/side-effect! write-version-file!)
      (u/side-effect! git-add-version-file!)
      (u/side-effect! commit-version-file)))

;;----------------------------------------------------------------------------------------------------------------------
;; Build
;;----------------------------------------------------------------------------------------------------------------------
(def conf {:project/working-dir (u/safer-path)
           :version/scheme vs/simple-scheme
           :project/author "Jeremy Schoffen"})

(defn build! []
  (-> conf
      (c/get-state)
      (gs/check-not-dirty)
      (add-version-file!)
      (u/assoc-computed :new-tag gs/bump-tag!)))




(comment
  (defn version-file [ctxt]
    (let [next-version (gs/next-version ctxt)
          next-version (if (= next-version (vs/initial-version ctxt))
                         next-version
                         (update next-version :distance inc))]
      next-version))


  (build!)

  (-> conf
      (c/get-state)
      version-file)
  (->> conf
      (c/get-state)
      gs/next-version)

  (-> conf
      (c/get-state)
      (git/dirty?)))