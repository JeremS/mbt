(ns ^{:author "Jeremy Schoffen"
      :doc "
Default config pertaining to general project value.
      "}fr.jeremyschoffen.mbt.alpha.default.config.project
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
(defn deps-file
  "Default location of the `deps.edn` file base on `:...mbt.alpha.project/working-dir`."
  [{wd ::project/working-dir}]
  (u/safer-path wd "deps.edn"))

(u/spec-op deps-file
           :param {:req [::project/working-dir]}
           :ret ::project.deps/file)

(defn get-deps [conf]
  "Get the project's deps and merge into it the maven repos found in user and system deps."
  (assoc (mbt-core/deps-get conf)
    :mvn/repos (:mvn/repos (mbt-core/deps-get-all conf))))


(u/spec-op get-deps
           :deps [mbt-core/deps-get mbt-core/deps-get-all]
           :param {:req [::project.deps/file]}
           :ret ::project/deps)


(def conf {::project/working-dir (impl/calc working-dir)
           ::project/output-dir (impl/calc output-dir ::project/working-dir)
           ::project/author (impl/calc project-author)
           ::project/name  (impl/calc project-name ::project/working-dir)
           ::project.deps/file (impl/calc deps-file ::project/working-dir)
           ::project/deps (impl/calc get-deps ::project.deps/file)})
