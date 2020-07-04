(ns build
  (:require
    [clojure.spec.test.alpha :as spec-test]
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
    (if-not current-version
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

(defn git-commit-version-file! [param]
  (mbt-core/git-commit! (assoc param
                          :git/commit! {:git.commit/message "Committed version file."})))


(defn add-version-file! [ctxt]
  (-> ctxt
      (u/side-effect! write-version-file!)
      (u/side-effect! git-add-version-file!)
      (u/side-effect! git-commit-version-file!)))

;;----------------------------------------------------------------------------------------------------------------------
;; Build
;;----------------------------------------------------------------------------------------------------------------------
(defn tag-new-version! [param]
  (-> param
      (u/check mbt-defaults/check-repo-in-order)
      (u/side-effect! add-version-file!)
      (u/side-effect! mbt-defaults/bump-tag!)))


(defn build! [conf]
  (-> conf
      (u/assoc-computed :project/version (comp str mbt-defaults/current-version))
      mbt-defaults/ensure-jar-defaults
      mbt-defaults/jar!))


(comment
  (str (mbt-defaults/current-version conf))
  (str (next-version conf))
  (tag-new-version! conf)

  (build! conf))
