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
(def working-dir (constantly (u/safer-path)))

(u/spec-op working-dir
           :ret :project/working-dir)


(defn output-dir [{wd :project/working-dir}]
  (u/safer-path wd "target"))

(u/spec-op output-dir
           :param {}
           :ret :project/output-dir)


(defn project-author [_]
  (System/getProperty "user.name"))

(u/spec-op project-author
           :ret :project/author)


(declare group-id)


(defn project-name [{major :versioning/major
                     :as param}]
  (let [prefix (mbt-core/git-prefix param)
        base (if-not (-> prefix str seq)
               (str (group-id param))
               (->> prefix
                    (map str)
                    (string/join "-")))]
    (-> base
        (cond-> major (str "-" (name major))))))

(u/spec-op project-name
           :deps [group-id mbt-core/git-prefix]
           :param {:req [:project/working-dir]
                   :opt [:versioning/major]}
           :ret :project/name)


;;----------------------------------------------------------------------------------------------------------------------
;; Compilation
;;----------------------------------------------------------------------------------------------------------------------
(defn compilation-clojure-dir [{out :project/output-dir}]
  (fs/path out "classes"))

(u/spec-op compilation-clojure-dir
           :param {:req [:project/output-dir]}
           :ret :compilation.clojure/output-dir)


(defn compilation-java-dir [{out :project/output-dir}]
  (fs/path out "classes"))

(u/spec-op compilation-java-dir
           :param {:req [:project/output-dir]}
           :ret :compilation.java/output-dir)

;;----------------------------------------------------------------------------------------------------------------------
;; GPG
;;----------------------------------------------------------------------------------------------------------------------
(defn gpg-command [_]
  (gpg-defaults/default-gpg-command))

(u/spec-op gpg-command
           :ret (s/nilable :gpg/command))


(defn gpg-version [param]
  (when (:gpg/command param)
    (mbt-core/gpg-version param)))

(u/spec-op gpg-version
           :param {:opt [:gpg/command]}
           :ret (s/nilable :gpg/version))


;;----------------------------------------------------------------------------------------------------------------------
;; Git
;;----------------------------------------------------------------------------------------------------------------------
(defn git-repo [param]
  (mbt-core/git-make-jgit-repo param))

(u/spec-op git-repo
           :deps [mbt-core/git-make-jgit-repo]
           :param {:req [:project/working-dir]}
           :ret :git/repo)


;;----------------------------------------------------------------------------------------------------------------------
;; Cleaning
;;----------------------------------------------------------------------------------------------------------------------
(defn cleaning-target [param]
  (:project/output-dir param))

(u/spec-op cleaning-target
           :param {:req [:project/output-dir]}
           :ret :cleaning/target)


;;----------------------------------------------------------------------------------------------------------------------
;; Maven
;;----------------------------------------------------------------------------------------------------------------------
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


(defn artefact-name [param]
  (let [p-name (:project/name param (project-name param))]
    (symbol p-name)))

(u/spec-op artefact-name
           :deps [project-name]
           :param {:req [:project/working-dir]
                   :opt [:versioning/major]}
           :ret :maven/artefact-name)


(defn pom-dir [{wd :project/output-dir}]
  wd)

(u/spec-op pom-dir
           :param {:req [:project/output-dir]}
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
  (fs/path mbt-core/maven-default-settings-file))

(u/spec-op maven-settings-file
           :ret :maven.settings/file)


;;----------------------------------------------------------------------------------------------------------------------
;; Jar building
;;----------------------------------------------------------------------------------------------------------------------
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
  (:project/name param (project-name param)))

(u/spec-op tag-base-name
           :deps [project-name]
           :param {:req [:project/working-dir]
                   :opt [:versioning/major]}
           :ret :versioning/tag-base-name)


;;----------------------------------------------------------------------------------------------------------------------
;; Putting it all together
;;----------------------------------------------------------------------------------------------------------------------
(def ^:private ctxt-building-scheme
  [:project/working-dir working-dir
   :project/output-dir output-dir
   :project/author project-author
   :project/name project-name

   :compilation.java/output-dir compilation-java-dir
   :compilation.clojure/output-dir compilation-clojure-dir

   :gpg/command gpg-command
   :gpg/version gpg-version

   :git/repo git-repo

   :cleaning/target cleaning-target

   :maven/artefact-name artefact-name
   :maven/group-id group-id
   :maven.pom/dir pom-dir
   :maven/local-repo maven-local-repo
   :maven.install/dir maven-install-dir
   :maven.settings/file maven-settings-file

   :build/jar-name jar-name
   :build/uberjar-name uberjar-name

   :versioning/tag-base-name tag-base-name])


(defn make-context
  "Make a config usable by mbt's apis. The `user-defined` parameter must be a map of configuration. Any key not present
  in `user-defined` will be set to a default value."
  [user-defined]
  (->> (apply u/ensure-computed user-defined ctxt-building-scheme)
       (medley/filter-vals identity)))

(comment
  (into (sorted-map)
        (make-context {:project/working-dir (u/safer-path "resources-test" "test-repos" "monorepo" "project1")
                       :versioning/major :alpha})))