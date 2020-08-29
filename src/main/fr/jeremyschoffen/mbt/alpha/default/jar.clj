(ns ^{:author "Jeremy Schoffen"
      :doc "
Apis providing the jar sources used by default.
      "}
  fr.jeremyschoffen.mbt.alpha.default.jar
  (:require
    [clojure.spec.alpha :as s]
    [clojure.data.xml :as xml]
    [fr.jeremyschoffen.java.nio.alpha.file :as fs]
    [fr.jeremyschoffen.mbt.alpha.core :as mbt-core]
    [fr.jeremyschoffen.mbt.alpha.core.specs]
    [fr.jeremyschoffen.mbt.alpha.default.specs]
    [fr.jeremyschoffen.mbt.alpha.utils :as u]))


(u/pseudo-nss
  build
  build.jar
  build.uberjar
  classpath
  cleaning
  jar
  jar.entry
  jar.manifest
  maven
  maven.pom
  project
  project.deps
  project.license)


;;----------------------------------------------------------------------------------------------------------------------
;; Jar staple srcs construction
;;----------------------------------------------------------------------------------------------------------------------
;; Manifest
(def meta-dir "META-INF")
(def manifest-name "MANIFEST.MF")
(def manifest-path (fs/path meta-dir manifest-name))

(defn make-manifest-entry
  "Make a `:fr...mbt.alpha.jar/entry` for a jar manifest."
  [{manifest ::jar/manifest}]
  {::jar.entry/src manifest
   ::jar.entry/dest manifest-path})

(u/spec-op make-manifest-entry
           :param {:req [::jar/manifest]}
           :ret ::jar/entry)

;;----------------------------------------------------------------------------------------------------------------------
;; Pom
(def maven-dir "maven")


(defn- make-jar-maven-path [group-id artefact-id]
  (fs/path meta-dir maven-dir (str group-id) (str artefact-id)))


(defn- make-pom-path [group-id artefact-id]
  (fs/path (make-jar-maven-path group-id artefact-id)
           "pom.xml"))

(defn make-pom-entry
  "Make a `:fr...mbt.alpha.jar/entry` for a `pom.xml` file."
  [{pom ::maven.pom/xml
    group-id ::maven/group-id
    artefact-id ::maven/artefact-name}]
  {::jar.entry/src (xml/indent-str pom)
   ::jar.entry/dest (make-pom-path group-id artefact-id)})

(u/spec-op make-pom-entry
           :param {:req [::maven.pom/xml
                         ::maven/group-id
                         ::maven/artefact-name]})


(defn- make-pom-props-path [group-id artefact-id]
  (fs/path (make-jar-maven-path group-id artefact-id)
           "pom.properties"))


(defn make-pom-properties-entry [{pom-props ::maven.pom/properties
                                  group-id ::maven/group-id
                                  artefact-id ::maven/artefact-name}]
  {::jar.entry/src pom-props
   ::jar.entry/dest (make-pom-props-path group-id artefact-id)})

(u/spec-op make-pom-properties-entry
           :param {:req [::maven.pom/properties
                         ::maven/group-id
                         ::maven/artefact-name]})


;;----------------------------------------------------------------------------------------------------------------------
;; Deps.edn
(def deps-dir "deps")


(defn- make-jar-deps-path [group-id artefact-id]
  (fs/path meta-dir deps-dir (str group-id) (str artefact-id) "deps.edn"))


(defn make-deps-entry
  "Make a `:fr...mbt.alpha.jar/entry` for a `deps.edn` file."
  [{deps ::project/deps
    group-id ::maven/group-id
    artefact-id ::maven/artefact-name}]
  {::jar.entry/src (pr-str deps)
   ::jar.entry/dest (make-jar-deps-path group-id artefact-id)})

(u/spec-op make-deps-entry
           :param {:req [::project/deps
                         ::maven/group-id
                         ::maven/artefact-name]})


;;----------------------------------------------------------------------------------------------------------------------
;; License file
(defn- license-file->entry [group-id artefact-id p]
  (let [name (-> p fs/file-name str)]
    {::jar.entry/src  p
     ::jar.entry/dest (fs/path meta-dir "licenses" (str group-id) (str artefact-id) name)}))


(defn make-license-entries [{group-id ::maven/group-id
                             artefect-name ::maven/artefact-name
                             licenses ::project/licenses}]
  (into []
        (comp
          (keep ::project.license/file)
          (map #(license-file->entry group-id artefect-name %)))
        (or licenses [])))


;; Pom + manifest + deps
(defn make-staples-entries
  "Make a `:fr...mbt.alpha.jar/src` containing the usual manifest, pom.xml and deps.edn `jar/entry`s."
  [param]
  (into [(-> param make-manifest-entry)
         (-> param make-pom-entry)
         (-> param make-pom-properties-entry)
         (-> param make-deps-entry)]

        (make-license-entries param)))

(u/spec-op make-staples-entries
           :deps [make-manifest-entry
                  make-pom-entry
                  make-pom-properties-entry
                  make-deps-entry
                  make-license-entries]
           :param {:req [::jar/manifest
                         ::maven/artefact-name
                         ::maven/group-id
                         ::maven.pom/xml
                         ::maven.pom/properties
                         ::project/deps]
                   :opt [::project/licenses]}
           :ret ::jar/src)

;;----------------------------------------------------------------------------------------------------------------------
;; Src from classpath
;;----------------------------------------------------------------------------------------------------------------------
(defn- warn-wayward-files [{cp ::classpath/index}]
  (let [wayward-files (::classpath/file cp)]
    (when-let [files (seq wayward-files)]
      (binding [*out* *err*]
        (println "Wayward files on classpath")
        (doseq [f files]
          (println f))))))


(defn- classpath->sources [cp ks]
  (-> cp
      (select-keys ks)
      vals
      (->> (into [] (comp
                      cat
                      (map u/safer-path))))))


(defn simple-jar-srcs
  "Make the jar srcs used in a skinny jar. Basically all the project local sources and resources directories present in
  the classpath."
  [{cp ::classpath/index
    :as param}]
  (warn-wayward-files param)
  (into [(make-staples-entries param)]
        (classpath->sources cp #{::classpath/dir})))

(u/spec-op simple-jar-srcs
           :deps [make-staples-entries]
           :param {:req [::classpath/index
                         ::jar/manifest
                         ::maven/artefact-name
                         ::maven/group-id
                         ::maven.pom/xml
                         ::maven.pom/properties
                         ::project/deps]
                   :opt [::project/licenses]}
           :ret ::jar/srcs)


(defn uber-jar-srcs
  "Make the jar srcs that will go into an uberjar. Similar to
  [[fr.jeremyschoffen.mbt.alpha.default.jar/simple-jar-srcs]] but also adds the other sources present in the classpath."
  [{cp ::classpath/index
    :as param}]
  (warn-wayward-files param)
  (into [(make-staples-entries param)]
        (classpath->sources cp #{::classpath/dir
                                 ::classpath/ext-dep
                                 ::classpath/jar})))

(u/spec-op uber-jar-srcs
           :deps [make-staples-entries classpath->sources]
           :param {:req [::classpath/index
                         ::jar/manifest
                         ::maven/artefact-name
                         ::maven/group-id
                         ::maven.pom/xml
                         ::maven.pom/properties
                         ::project/deps]
                   :opt [:fr.jeremyschoffen.mbt.alpha.project/licenses]}
           :ret ::jar/srcs)


;;----------------------------------------------------------------------------------------------------------------------
;; Jar building
;;----------------------------------------------------------------------------------------------------------------------
(defn ensure-jar-defaults
  "Ensure the presence in the config map of some necessary keys to make a jar. Computes
   values for those keys if necessary.

   Namely:
    - :fr...mbt.alpha.classpath/index
    - :fr...mbt.alpha.maven.pom/xml
    - :fr...mbt.alpha.maven.pom/properties
    - :fr...mbt.alpha.jar/manifest"
  [p]
  (u/ensure-computed p
    ::classpath/index mbt-core/classpath-indexed
    ::maven.pom/xml mbt-core/maven-sync-pom!
    ::maven.pom/properties mbt-core/maven-new-pom-properties
    ::jar/manifest mbt-core/manifest))

(u/spec-op ensure-jar-defaults
           :deps [mbt-core/classpath-indexed
                  mbt-core/maven-sync-pom!
                  mbt-core/maven-new-pom-properties
                  mbt-core/manifest]
           :param {:req #{::maven/artefact-name
                          ::maven/group-id
                          ::maven.pom/path
                          ::project/deps
                          ::project/working-dir
                          ::project/version}
                   :opt #{::jar/main-ns
                          ::jar.manifest/overrides
                          ::maven/scm
                          ::project/author
                          ::project.deps/aliases
                          ::project/licenses}}
           :ret (s/keys :req [::classpath/index
                              ::maven.pom/xml
                              ::maven.pom/properties
                              ::jar/manifest]))


(defn make-jar&clean!
  "Create a jar, simplifying the process by handling the creation and deletion of the temporary output put that will
  be zipped into the resulting jar."
  [{jar-out ::jar/output
    :as param}]
  (let [res (atom nil)
        out-dir (-> jar-out fs/parent u/ensure-dir!)
        _ (fs/delete-if-exists! jar-out)

        temp-out (fs/create-temp-directory! "temp-out_" {:dir out-dir})]
    (-> param
        (assoc ::jar/temp-output temp-out
               ::cleaning/target temp-out)
        (u/side-effect! #(reset! res (mbt-core/jar-add-srcs! %)))
        (u/do-side-effect! mbt-core/jar-make-archive!)
        (u/do-side-effect! mbt-core/clean!))
    @res))

(u/spec-op make-jar&clean!
           :deps [mbt-core/jar-add-srcs!
                  mbt-core/jar-make-archive!
                  mbt-core/clean!]
           :param {:req [::jar/output
                         ::jar/srcs
                         ::project/working-dir]
                   :opt [::jar/exclude?]})


(defn jar!
  "Create a skinny jar. The jar sources are determined using
  [[fr.jeremyschoffen.mbt.alpha.default.jar/simple-jar-srcs]].

  This function takes care of generating a deleting a temporary directory used to group the
  jar files that end up compressed into the jar archive."
  [param]
  (-> param
      (u/assoc-computed ::jar/output ::build.jar/path)
      (u/ensure-computed ::jar/srcs simple-jar-srcs)
      make-jar&clean!))

(u/spec-op jar!
           :deps [simple-jar-srcs
                  make-jar&clean!]
           :param {:req [::build.jar/path
                         ::classpath/index
                         ::jar/manifest
                         ::maven/artefact-name
                         ::maven/group-id
                         ::maven.pom/xml
                         ::maven.pom/properties
                         ::project/deps
                         ::project/working-dir]
                   :opt [::jar/exclude?
                         ::project/licences]})


(defn uberjar!
  "Build an uberjar. The jar sources are determined using
  [[fr.jeremyschoffen.mbt.alpha.default.jar/uber-jar-srcs]].

  This function takes care of generating a deleting a temporary directory used to group the
  jar files that end up compressed into the jar archive."
  [param]
  (-> param
      (u/ensure-computed ::jar/srcs uber-jar-srcs)
      (u/assoc-computed ::jar/output ::build.uberjar/path)
      make-jar&clean!))

(u/spec-op uberjar!
           :deps [uber-jar-srcs
                  make-jar&clean!]
           :param {:req [::build.uberjar/path
                         ::classpath/index
                         ::jar/manifest
                         ::maven/artefact-name
                         ::maven/group-id
                         ::maven.pom/xml
                         ::maven.pom/properties
                         ::project/deps
                         ::project/working-dir]
                   :opt [::jar/exclude?
                         ::project/licenses]})
