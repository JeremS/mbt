(ns com.jeremyschoffen.mbt.alpha.default.defaults
  (:require
    [clojure.string :as string]
    [clojure.tools.deps.alpha.util.maven :as deps-maven]
    [com.jeremyschoffen.java.nio.file :as fs]
    [com.jeremyschoffen.mbt.alpha.core :as mbt-core]
    [com.jeremyschoffen.mbt.alpha.core.specs]
    [com.jeremyschoffen.mbt.alpha.default.specs]
    [com.jeremyschoffen.mbt.alpha.utils :as u]))


(def working-dir (constantly (u/safer-path)))

(u/spec-op working-dir
           :ret :project/working-dir)


(defn output-dir [{wd :project/working-dir}]
  (u/safer-path wd "target"))


(u/spec-op output-dir
           :param {}
           :ret :project/output-dir)


(defn compilation-dir [{out :project/output-dir}]
  (fs/path out "classes"))

(u/spec-op compilation-dir
           :param {:req [:project/output-dir]}
           :ret :clojure.compilation/output-dir)


(defn cleaning-target [param]
  (:project/output-dir param))

(u/spec-op cleaning-target
           :param {:req [:project/output-dir]}
           :ret :cleaning/target)


(defn project-author [_]
  (System/getProperty "user.name"))

(u/spec-op project-author
           :ret :project/author)


(defn group-id [param]
  (-> param
      mbt-core/git-top-level
      fs/file-name
      str
      symbol))

(u/spec-op group-id
           :deps [mbt-core/git-top-level]
           :param {:req [:project/working-dir]}
           :ret :maven/group-id)

;; TODO: add something la :project/major with value like alpha and beta, reflecting the versioned part of the nss
(defn artefact-name [param]
  (let [prefix (mbt-core/git-prefix param)]
    (if-not (-> prefix str seq)
      (group-id param)
      (->> prefix
           (map str)
           (string/join "-")
           symbol))))

(u/spec-op artefact-name
           :deps [group-id mbt-core/git-prefix]
           :param {:req [:project/working-dir]}
           :ret :maven/artefact-name)


(defn pom-dir [{wd :project/working-dir}]
  wd)

(u/spec-op pom-dir
           :param {:req [:project/working-dir]}
           :ret :maven.pom/dir)

(defn maven-local-repo [_]
  (fs/path deps-maven/default-local-repo))

(u/spec-op maven-local-repo
           :ret :maven/local-repo)


(defn maven-install-dir [_]
  (fs/path deps-maven/default-local-repo))

(u/spec-op maven-install-dir
           :ret :maven.install/dir)


(defn maven-settings-file [_]
  (fs/path u/maven-default-settings-file))

(u/spec-op maven-settings-file
           :ret :maven.settings/file)

;; TODO: See if the :maven/server conf needs to have defaults

(defn jar-name [{artefact-name :maven/artefact-name}]
  (str artefact-name ".jar"))

(u/spec-op jar-name
           :param {:req [:maven/artefact-name]}
           :ret :build/jar-name)


(defn uberjar-name [{artefact-name :maven/artefact-name}]
  (str artefact-name "-standalone.jar"))

(u/spec-op uberjar-name
           :param {:req [:maven/artefact-name]}
           :ret :build/uberjar-name)


(defn tag-base-name [param]
  (-> param
      artefact-name
      str))

(u/spec-op tag-base-name
           :deps [artefact-name]
           :param {:req [:project/working-dir]}
           :ret :versioning/tag-base-name)


(def ^:private ctxt-building-scheme
  [:project/working-dir working-dir
   :project/output-dir output-dir
   :project/author project-author

   :clojure.compilation/output-dir compilation-dir

   :maven/artefact-name artefact-name
   :maven/group-id group-id
   :maven.pom/dir pom-dir
   :maven/local-repo maven-local-repo
   :maven.install/dir maven-install-dir
   :maven.settings/file maven-settings-file

   :build/jar-name jar-name
   :build/uberjar-name uberjar-name

   :versioning/tag-base-name tag-base-name])


(defn make-context [user-defined]
  (->> (apply u/ensure-computed user-defined ctxt-building-scheme)))

(into (sorted-map)
      (make-context {:project/working-dir (u/safer-path "test-repos" "monorepo" "project1")}))


