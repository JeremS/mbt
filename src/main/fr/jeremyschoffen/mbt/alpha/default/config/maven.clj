(ns ^{:author "Jeremy Schoffen"
      :doc "
Default config pertaining to maven utilities.
      "}fr.jeremyschoffen.mbt.alpha.default.config.maven
  (:require
    [fr.jeremyschoffen.java.nio.alpha.file :as fs]
    [fr.jeremyschoffen.mbt.alpha.core :as mbt-core]
    [fr.jeremyschoffen.mbt.alpha.default.specs]
    [fr.jeremyschoffen.mbt.alpha.default.config.impl :as impl]
    [fr.jeremyschoffen.mbt.alpha.default.maven :as default-maven]
    [fr.jeremyschoffen.mbt.alpha.utils :as u]))

(u/pseudo-nss
  maven
  maven.install
  maven.deploy
  maven.pom
  maven.settings
  project
  versioning)

(defn group-id
  "Default maven group-id: name of the git top level dir.

  See [[fr.jeremyschoffen.mbt.alpha.core/git-top-level]]."
  [param]
  (-> param
      mbt-core/git-top-level
      fs/file-name
      str
      symbol))

(u/spec-op group-id
           :deps [mbt-core/git-top-level]
           :param {:req [::project/working-dir]}
           :ret ::maven/group-id)


(defn artefact-name
  "Defaults to `project/name` + suffixes depending on `:versioning/major` and `:versioning/stable`."
  [{p-name  ::project/name
    major   ::versioning/major}]
  (-> p-name
      (cond-> (and major (not= major :none))
              (str "-" (name major)))
      symbol))

(u/spec-op artefact-name
           :param {:req [::project/name
                         ::versioning/major]}
           :ret ::maven/artefact-name)


(defn pom-path
  "Defaults to `:project/output-dir`."
  [{out-dir ::project/output-dir}]
  (u/safer-path out-dir "pom.xml"))

(u/spec-op pom-path
           :param {:req [::project/output-dir]}
           :ret ::maven.pom/path)


(defn maven-local-repo
  "The usual \"~/m2/repository\"."
  [& _]
  mbt-core/maven-default-local-repo)

(u/spec-op maven-local-repo
           :ret ::maven/local-repo)


(defn maven-install-dir
  "The usual \"~/m2/repository\"."
  [& _]
  mbt-core/maven-default-local-repo)

(u/spec-op maven-install-dir
           :ret ::maven.install/dir)


(defn maven-settings-file
  "The usual \"~/m2/settings.xml\"."
  [& _]
  (fs/path mbt-core/maven-default-settings-file))

(u/spec-op maven-settings-file
           :ret ::maven.settings/file)



(def conf {::maven/group-id (impl/calc group-id ::project/working-dir)
           ::maven/artefact-name (impl/calc artefact-name ::project/name ::versioning/major)
           ::maven.pom/path (impl/calc pom-path ::project/output-dir)
           ::maven/local-repo (impl/calc maven-local-repo)
           ::maven.install/dir (impl/calc maven-install-dir)
           ::maven.settings/file (impl/calc maven-settings-file)
           ::maven/scm (impl/calc default-maven/make-github-like-scm-map ::project/git-url)

           ::maven.deploy/sign-artefacts? false})
