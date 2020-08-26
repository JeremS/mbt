(ns fr.jeremyschoffen.mbt.alpha.default.config.project
  (:require
    [clojure.string :as string]
    [fr.jeremyschoffen.java.nio.alpha.file :as fs]
    [fr.jeremyschoffen.mbt.alpha.core :as mbt-core]
    [fr.jeremyschoffen.mbt.alpha.default.specs]
    [fr.jeremyschoffen.mbt.alpha.default.config.impl :as impl]
    [fr.jeremyschoffen.mbt.alpha.utils :as u]))

(u/pseudo-nss
  project
  project.deps
  versioning)

(def working-dir
  "Default working dir is the current user dir."
  (constantly (u/safer-path)))

(u/spec-op working-dir
           :ret ::project/working-dir)


(defn output-dir
  "Default output dir \"working-dir/target\""
  [{wd ::project/working-dir}]
  (u/safer-path wd "target"))

(u/spec-op output-dir
           :param {}
           :ret ::project/output-dir)


(defn project-author
  "User name."
  [& _]
  (System/getProperty "user.name"))

(u/spec-op project-author
           :ret ::project/author)


(defn project-name
  "Based on git prefix if there is one, if not name of the git top-level"
  [param]
  (let [prefix (mbt-core/git-prefix param)
        top-level (mbt-core/git-top-level param)]
    (if-not (-> prefix str seq)
      (-> top-level fs/file-name str)
      (->> prefix
           (map str)
           (string/join "-")))))

(u/spec-op project-name
           :deps [mbt-core/git-prefix]
           :param {:req [::project/working-dir]}
           :ret ::project/name)


;;----------------------------------------------------------------------------------------------------------------------
;; Deps
;;----------------------------------------------------------------------------------------------------------------------
(defn deps-file [{wd ::project/working-dir}]
  (u/safer-path wd "deps.edn"))

(u/spec-op deps-file
           :param {:req [::project/working-dir]}
           :ret ::project.deps/file)


(def conf {::project/working-dir (impl/calc working-dir)
           ::project/output-dir (impl/calc output-dir ::project/working-dir)
           ::project/author (impl/calc project-author)
           ::project/name  (impl/calc project-name ::project/working-dir)
           ::project.deps/file (impl/calc deps-file ::project/working-dir)})
