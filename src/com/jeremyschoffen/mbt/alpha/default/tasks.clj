(ns com.jeremyschoffen.mbt.alpha.default.tasks
  (:require
    [com.jeremyschoffen.java.nio.alpha.file :as fs]
    [com.jeremyschoffen.mbt.alpha.core :as mbt-core]
    [com.jeremyschoffen.mbt.alpha.default.building :as b]
    [com.jeremyschoffen.mbt.alpha.default.versioning :as v]
    [com.jeremyschoffen.mbt.alpha.default.specs]
    [com.jeremyschoffen.mbt.alpha.utils :as u]))


;;----------------------------------------------------------------------------------------------------------------------
;; Version file
;;----------------------------------------------------------------------------------------------------------------------
(defn anticipated-next-version [param]
  (let [current-version (v/current-version param)]
    (if-not current-version
      (v/schemes-initial-version param)
      (-> param
          (assoc :versioning/version
                 (update current-version :distance inc))
          v/schemes-bump))))

(u/spec-op anticipated-next-version
           :deps [v/current-version v/schemes-initial-version v/schemes-bump]
           :param {:req [:git/repo
                         :versioning/scheme]
                   :opt [:versioning/bump-level
                         :versioning/tag-base-name]}
           :ret :versioning/version)


(defn- write-version-file! [param]
  (-> param
      (u/assoc-computed :project/version v/current-project-version)
      (v/write-version-file!)))

(u/spec-op write-version-file!
           :deps [anticipated-next-version v/write-version-file!]
           :param {:req [:git/repo
                         :version-file/ns
                         :version-file/path
                         :versioning/scheme]
                   :opt [:versioning/bump-level
                         :versioning/tag-base-name]})


(defn- git-add-version-file! [{p :version-file/path
                               repo :git/repo}]
  (mbt-core/git-add! {:git/repo repo
                      :git/add! {:git.add!/file-patterns [(->> p
                                                               (fs/relativize repo)
                                                               str)]}}))

(u/spec-op git-add-version-file!
           :param {:req [:git/repo :version-file/path]})



(defn- git-commit-version-file! [param]
  (mbt-core/git-commit! (assoc param
                          :git/commit! {:git.commit/message "Committed version file."})))

(u/spec-op git-commit-version-file!)


(defn add-version-file! [ctxt]
  (-> ctxt
      (u/check v/check-repo-in-order)
      (u/side-effect! write-version-file!)
      (u/side-effect! git-add-version-file!)
      (u/side-effect! git-commit-version-file!)))

(u/spec-op add-version-file!
           :deps [v/check-repo-in-order
                  write-version-file!
                  git-add-version-file!
                  git-commit-version-file!]
           :param {:req [:git/repo
                         :project/working-dir
                         :version-file/ns
                         :version-file/path
                         :versioning/scheme]
                   :opt [:versioning/bump-level
                         :versioning/tag-base-name]})

;;----------------------------------------------------------------------------------------------------------------------
;; Building jars
;;----------------------------------------------------------------------------------------------------------------------
(defn jar! [param]
  (-> param
      (u/ensure-computed :project/version v/current-project-version)
      b/ensure-jar-defaults
      b/jar!))

(u/spec-op jar!
           :deps [v/current-version b/ensure-jar-defaults b/jar!]
           :param {:req [:build/jar-name
                         :git/repo
                         :maven/artefact-name
                         :maven/group-id
                         :project/output-dir
                         :project/working-dir
                         :versioning/scheme]
                   :opt [:jar/exclude?
                         :jar/main-ns
                         :jar.manifest/overrides
                         :project/author
                         :project.deps/aliases
                         :versioning/tag-base-name]})


(defn uberjar! [param]
  (-> param
      (u/ensure-computed :project/version (comp str v/current-version))
      b/ensure-jar-defaults
      b/uberjar!))

(u/spec-op uberjar!
           :deps [v/current-version b/ensure-jar-defaults b/uberjar!]
           :param {:req [:build/uberjar-name
                         :git/repo
                         :maven/artefact-name
                         :maven/group-id
                         :project/output-dir
                         :project/working-dir
                         :versioning/scheme]
                   :opt [:jar/exclude?
                         :jar/main-ns
                         :jar.manifest/overrides
                         :project/author
                         :project.deps/aliases
                         :versioning/tag-base-name]})