(ns ^{:author "Jeremy Schoffen"
      :doc "
Api providing the default generation of the build configuration.
      "}
  fr.jeremyschoffen.mbt.alpha.default.defaults
  (:require
    [clojure.string :as string]
    [clojure.tools.deps.alpha.util.maven :as deps-maven]
    [medley.core :as medley]
    [fr.jeremyschoffen.java.nio.alpha.file :as fs]

    [fr.jeremyschoffen.mbt.alpha.core :as mbt-core]
    [fr.jeremyschoffen.mbt.alpha.core.specs]
    [fr.jeremyschoffen.mbt.alpha.default.defaults.gpg :as gpg-defaults]
    [fr.jeremyschoffen.mbt.alpha.default.specs]
    [fr.jeremyschoffen.mbt.alpha.utils :as u]
    [clojure.spec.alpha :as s]))

;;----------------------------------------------------------------------------------------------------------------------
;; General project values
;;----------------------------------------------------------------------------------------------------------------------
(def working-dir
  "Default working dir is the current user dir."
  (constantly (u/safer-path)))

(u/spec-op working-dir
           :ret :project/working-dir)


(defn output-dir
  "Default output dir \"working-dir/target\""
  [{wd :project/working-dir}]
  (u/safer-path wd "target"))

(u/spec-op output-dir
           :param {}
           :ret :project/output-dir)


(defn project-author
  "User name."
  [_]
  (System/getProperty "user.name"))

(u/spec-op project-author
           :ret :project/author)


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
           :param {:req [:project/working-dir]
                   :opt [:versioning/major]}
           :ret :project/name)

;;----------------------------------------------------------------------------------------------------------------------
;; Deps
;;----------------------------------------------------------------------------------------------------------------------
(defn deps-file [{wd :project/working-dir}]
  (u/safer-path wd "deps.edn"))

(u/spec-op deps-file
           :param {:req [:project/working-dir]}
           :ret :project/deps-file)

;;----------------------------------------------------------------------------------------------------------------------
;; Compilation
;;----------------------------------------------------------------------------------------------------------------------
(defn compilation-clojure-dir
  "The default clojure compilation directory: \"output-dir/classes\""
  [{out :project/output-dir}]
  (fs/path out "classes"))

(u/spec-op compilation-clojure-dir
           :param {:req [:project/output-dir]}
           :ret :compilation.clojure/output-dir)


(defn compilation-java-dir
  "The default java compilation directory: \"output-dir/classes\""
  [{out :project/output-dir}]
  (fs/path out "classes"))

(u/spec-op compilation-java-dir
           :param {:req [:project/output-dir]}
           :ret :compilation.java/output-dir)

;;----------------------------------------------------------------------------------------------------------------------
;; GPG
;;----------------------------------------------------------------------------------------------------------------------
(defn gpg-command
  "Default gpg command, \"gpg2\" or \"gpg\" as long as one is present on the system."
  [_]
  (gpg-defaults/default-gpg-command))

(u/spec-op gpg-command
           :ret (s/nilable :gpg/command))


(defn gpg-version
  "Look up the gpg version currently installed. See [[fr.jeremyschoffen.mbt.alpha.core/gpg-version]]."
  [param]
  (when (:gpg/command param)
    (mbt-core/gpg-version param)))

(u/spec-op gpg-version
           :param {:opt [:gpg/command]}
           :ret (s/nilable :gpg/version))


;;----------------------------------------------------------------------------------------------------------------------
;; Git
;;----------------------------------------------------------------------------------------------------------------------
(defn git-repo
  "See [[fr.jeremyschoffen.mbt.alpha.core/git-make-jgit-repo]]."
  [param]
  (mbt-core/git-make-jgit-repo param))

(u/spec-op git-repo
           :deps [mbt-core/git-make-jgit-repo]
           :param {:req [:project/working-dir]}
           :ret :git/repo)


;;----------------------------------------------------------------------------------------------------------------------
;; Cleaning
;;----------------------------------------------------------------------------------------------------------------------
(defn cleaning-target
  "Default cleaning dir -> default output-dir."
  [param]
  (:project/output-dir param))

(u/spec-op cleaning-target
           :param {:req [:project/output-dir]}
           :ret :cleaning/target)


;;----------------------------------------------------------------------------------------------------------------------
;; Versioning
;;----------------------------------------------------------------------------------------------------------------------
(defn stable
  "True by default, defines whether the project is stable. Influences tag and artefact name."
  [_]
  true)

(u/spec-op stable
           :ret :versioning/stable)

(defn tag-base-name
  "Defaults to `project/name` + suffixes depending on `:versioning/major` and `:versioning/stable`."
  [{p-name  :project/name
    major   :versioning/major
    stable? :versioning/stable}]
  (-> p-name
      (cond-> major   (str "-" (name major))
              (not stable?) (str "-unstable"))))

(u/spec-op tag-base-name
           :deps [project-name]
           :param {:req [:project/name
                         :versioning/stable]
                   :opts [:versioning/major]}
           :ret :versioning/tag-base-name)
;;----------------------------------------------------------------------------------------------------------------------
;; Maven
;;----------------------------------------------------------------------------------------------------------------------
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
           :param {:req [:project/working-dir]}
           :ret :maven/group-id)


(defn artefact-name
  "Default maven name: `project/name`."
  [{base-name :versioning/tag-base-name}]
  (symbol base-name))


(u/spec-op artefact-name
           :deps [project-name]
           :param {:req [:versioning/tag-base-name]}
           :ret :maven/artefact-name)


(defn pom-dir
  "Defaults to `:project/output-dir`."
  [{wd :project/output-dir}]
  wd)

(u/spec-op pom-dir
           :param {:req [:project/output-dir]}
           :ret :maven.pom/dir)


(defn maven-local-repo
  "The usual \"~/m2/repository\"."
  [_]
  (fs/path deps-maven/default-local-repo))

(u/spec-op maven-local-repo
           :ret :maven/local-repo)


(defn maven-install-dir
  "The usual \"~/m2/repository\"."
  [_]
  (fs/path deps-maven/default-local-repo))

(u/spec-op maven-install-dir
           :ret :maven.install/dir)


(defn maven-settings-file
  "The usual \"~/m2/settings.xml\"."
  [_]
  (fs/path mbt-core/maven-default-settings-file))

(u/spec-op maven-settings-file
           :ret :maven.settings/file)


;;----------------------------------------------------------------------------------------------------------------------
;; Jar building
;;----------------------------------------------------------------------------------------------------------------------
(defn jar-name
  "\"artefact-name.jar\""
  [{artefact-name :maven/artefact-name}]
  (str artefact-name ".jar"))

(u/spec-op jar-name
           :param {:req [:maven/artefact-name]}
           :ret :build/jar-name)


(defn uberjar-name
  "\"artefact-name-standalone.jar\""
  [{artefact-name :maven/artefact-name}]
  (str artefact-name "-standalone.jar"))

(u/spec-op uberjar-name
           :param {:req [:maven/artefact-name]}
           :ret :build/uberjar-name)





;;----------------------------------------------------------------------------------------------------------------------
;; Putting it all together
;;----------------------------------------------------------------------------------------------------------------------
(def ^:private ctxt-building-scheme
  [:project/working-dir working-dir
   :project/output-dir output-dir
   :project/author project-author
   :project/name project-name

   :project/deps-file deps-file

   :compilation.java/output-dir compilation-java-dir
   :compilation.clojure/output-dir compilation-clojure-dir

   :gpg/command gpg-command
   :gpg/version gpg-version

   :git/repo git-repo

   :cleaning/target cleaning-target

   :versioning/stable stable
   :versioning/tag-base-name tag-base-name

   :maven/group-id group-id
   :maven/artefact-name artefact-name
   :maven.pom/dir pom-dir
   :maven/local-repo maven-local-repo
   :maven.install/dir maven-install-dir
   :maven.settings/file maven-settings-file

   :build/jar-name jar-name
   :build/uberjar-name uberjar-name])




(defn make-context
  "Make a config usable by mbt's apis. The `user-defined` parameter must be a map of configuration. Any key not present
  in `user-defined` will be set to a default value."
  [user-defined]
  (apply u/ensure-computed user-defined ctxt-building-scheme))


(comment
  (-> (sorted-map
        :project/working-dir (u/safer-path "resources-test" "test-repos" "monorepo" "project1")
        :project/name "toto"
        :versioning/major :alpha
        :versioning/stable false)
      make-context))
      ;(select-keys #{:versioning/stable :versioning/tag-base-name})))
