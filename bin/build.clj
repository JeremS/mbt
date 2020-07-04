(ns build
  (:require
    [clojure.spec.test.alpha :as spec-test]
    [clojure.string :as string]
    [clj-jgit.porcelain :as jgit]
    [com.jeremyschoffen.java.nio.file :as fs]
    [com.jeremyschoffen.mbt.alpha.core :as mbt-core]
    [com.jeremyschoffen.mbt.alpha.default :as mbt-defaults]
    [com.jeremyschoffen.mbt.alpha.utils :as u]))


(spec-test/instrument)


(def conf (-> (sorted-map
                :project/working-dir (u/safer-path)
                :versioning/scheme mbt-defaults/simple-scheme
                :project/author "Jeremy Schoffen"
                :version-file/ns 'com.jeremyschoffen.mbt.alpha.version
                :version-file/path (u/safer-path "src" "com" "jeremyschoffen" "mbt" "alpha" "version.clj"))
              mbt-defaults/make-conf))


;;----------------------------------------------------------------------------------------------------------------------
;; utils
;;----------------------------------------------------------------------------------------------------------------------
(defn next-version [param]
  (let [current-version (mbt-defaults/current-version param)]
    (if (= current-version)
      (mbt-defaults/initial-version param)
      (-> param
          (assoc :versioning/version
                 (update current-version :distance inc))
          mbt-defaults/bump-version))))


(defn write-version-file! [param]
  (-> param
      (u/assoc-computed :project/version (comp str next-version))
      (mbt-defaults/write-version-file!)))


(defn git-add-version-file! [{p :version-file/path
                              repo :git/repo}]
  (mbt-core/git-add! {:git/repo repo
                      :git/add! {:git.add!/file-patterns [(->> p
                                                               (fs/relativize repo)
                                                               str)]}}))

(defn commit-version-file! [{repo :git/repo :as param}]
  (mbt-core/git-commit! (assoc param
                          :git/commit! {:git.commit/message "Committed version file."})))


(defn add-version-file! [ctxt]
  (-> ctxt
      (u/check mbt-defaults/check-repo-in-order)
      (u/side-effect! write-version-file!)
      (u/side-effect! git-add-version-file!)
      (u/side-effect! commit-version-file!)))

;;----------------------------------------------------------------------------------------------------------------------
;; Build
;;----------------------------------------------------------------------------------------------------------------------
(defn tag-new-version! [param]
  (-> param
      (u/side-effect! add-version-file!)
      (u/side-effect! mbt-defaults/bump-tag!)))

(comment
  (tag-new-version! conf))
