(ns ^{:author "Jeremy Schoffen"
      :doc "
Apis providing the jar sources used by default.
      "}
  fr.jeremyschoffen.mbt.alpha.default.jar
  (:require
    [clojure.data.xml :as xml]
    [fr.jeremyschoffen.java.nio.alpha.file :as fs]
    [fr.jeremyschoffen.mbt.alpha.core :as mbt-core]
    [fr.jeremyschoffen.mbt.alpha.core.specs]
    [fr.jeremyschoffen.mbt.alpha.default.specs]
    [fr.jeremyschoffen.mbt.alpha.utils :as u]))


;;----------------------------------------------------------------------------------------------------------------------
;; Jar staple srcs construction
;;----------------------------------------------------------------------------------------------------------------------
;; Manifest
(def meta-dir "META-INF")
(def manifest-name "MANIFEST.MF")
(def manifest-path (fs/path meta-dir manifest-name))

(defn make-manifest-entry
  "Make a `:jar/entry` for a jar manifest."
  [{manifest :jar/manifest}]
  {:jar.entry/src manifest
   :jar.entry/dest manifest-path})

(u/spec-op make-manifest-entry
           :param {:req [:jar/manifest]}
           :ret :jar/entry)

;;----------------------------------------------------------------------------------------------------------------------
;; Pom
(def maven-dir "maven")


(defn- make-jar-maven-path [group-id artefact-id]
  (fs/path meta-dir maven-dir (str group-id) (str artefact-id)))


(defn- make-pom-path [group-id artefact-id]
  (fs/path (make-jar-maven-path group-id artefact-id)
           "pom.xml"))

(defn make-pom-entry
  "Make a `:jar/entry` for a `pom.xml` file."
  [{pom :maven/pom
    group-id :maven/group-id
    artefact-id :maven/artefact-name}]
  {:jar.entry/src (xml/indent-str pom)
   :jar.entry/dest (make-pom-path group-id artefact-id)})

(u/spec-op make-pom-entry
           :param {:req [:maven/pom
                         :maven/group-id
                         :maven/artefact-name]})


(defn- make-pom-props-path [group-id artefact-id]
  (fs/path (make-jar-maven-path group-id artefact-id)
           "pom.properties"))


(defn make-pom-properties-entry [{pom-props :maven/pom-properties
                                  group-id :maven/group-id
                                  artefact-id :maven/artefact-name}]
  {:jar.entry/src pom-props
   :jar.entry/dest (make-pom-props-path group-id artefact-id)})

(u/spec-op make-pom-properties-entry
           :param {:req [:maven/pom-properties
                         :maven/group-id
                         :maven/artefact-name]})


;;----------------------------------------------------------------------------------------------------------------------
;; Deps.edn
(def deps-dir "deps")


(defn- make-jar-deps-path [group-id artefact-id]
  (fs/path meta-dir deps-dir (str group-id) (str artefact-id) "deps.edn"))


(defn make-deps-entry
  "Make a `:jar/entry` for a `deps.edn` file."
  [{deps :project/deps
    group-id :maven/group-id
    artefact-id :maven/artefact-name}]
  {:jar.entry/src (pr-str deps)
   :jar.entry/dest (make-jar-deps-path group-id artefact-id)})

(u/spec-op make-deps-entry
           :param {:req [:project/deps
                         :maven/group-id
                         :maven/artefact-name]})


;;----------------------------------------------------------------------------------------------------------------------
;; License file
(defn- license-file->entry [group-id artefact-id p]
  (let [name (-> p fs/file-name str)]
    {:jar.entry/src  p
     :jar.entry/dest (fs/path meta-dir "licenses" (str group-id) (str artefact-id) name)}))


(defn make-license-entries [{group-id :maven/group-id
                             artefect-name :maven/artefact-name
                             licenses :project/licenses
                             :as param}]
  (into []
        (comp
          (keep :project.license/file)
          (map #(license-file->entry group-id artefect-name %)))
        (or licenses [])))


;; Pom + manifest + deps
(defn make-staples-entries
  "Make a `:jar/src` containing the usual manifest, pom.xml and deps.edn `jar/entry`s."
  [param]
  (into [(-> param make-manifest-entry)
         (-> param make-pom-entry)
         (-> param make-pom-properties-entry)
         (-> param make-deps-entry)]

        (make-license-entries param)))

(u/spec-op make-staples-entries
           :deps [make-manifest-entry make-pom-entry make-deps-entry]
           :param {:req [:jar/manifest
                         :maven/artefact-name
                         :maven/group-id
                         :maven/pom
                         :maven/pom-properties
                         :project/deps]
                   :opt [:project/licenses]}
           :ret :jar/src)

;;----------------------------------------------------------------------------------------------------------------------
;; Src from classpath
;;----------------------------------------------------------------------------------------------------------------------
(defn- warn-wayward-files [{cp :classpath/index}]
  (let [wayward-files (:classpath/file cp)]
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
  [{cp :classpath/index
    :as param}]
  (warn-wayward-files param)
  (into [(make-staples-entries param)]
        (classpath->sources cp #{:classpath/dir})))

(u/spec-op simple-jar-srcs
           :deps [make-staples-entries]
           :param {:req [:classpath/index
                         :jar/manifest
                         :maven/artefact-name
                         :maven/group-id
                         :maven/pom
                         :maven/pom-properties
                         :project/deps]
                   :opt [:project/author
                         :jar/main-ns
                         :jar.manifest/overrides]}
           :ret :jar/srcs)


(defn uber-jar-srcs
  "Make the jar srcs that will go into an uberjar. Similar to
  [[fr.jeremyschoffen.mbt.alpha.default.jar/simple-jar-srcs]] but also adds the other sources present in the classpath."
  [{cp :classpath/index
    :as param}]
  (warn-wayward-files param)
  (into [(make-staples-entries param)]
        (classpath->sources cp #{:classpath/dir
                                 :classpath/ext-dep
                                 :classpath/jar})))

(u/spec-op uber-jar-srcs
           :deps [make-staples-entries classpath->sources]
           :param {:req [:classpath/index
                         :jar/manifest
                         :maven/artefact-name
                         :maven/group-id
                         :maven/pom
                         :maven/pom-properties
                         :project/deps]}
           :ret :jar/srcs)


;;----------------------------------------------------------------------------------------------------------------------
;; Jar building
;;----------------------------------------------------------------------------------------------------------------------
(defn ensure-jar-defaults
  "Add to a config map the necessary keys to make a jar. Namely:
    - :project/deps
    - :classpath/index
    - :maven/pom
    - :jar/manifest"
  [p]
  (u/ensure-computed p
                     :project/deps mbt-core/deps-get
                     :classpath/index mbt-core/classpath-indexed
                     :maven/pom mbt-core/maven-new-pom
                     :maven/pom-properties mbt-core/maven-new-pom-properties
                     :jar/manifest mbt-core/manifest))

(u/spec-op ensure-jar-defaults
           :deps [mbt-core/classpath-indexed
                  mbt-core/manifest
                  mbt-core/maven-new-pom
                  mbt-core/maven-new-pom-properties
                  mbt-core/deps-get]
           :param {:req #{:maven/artefact-name
                          :maven/group-id
                          :project/working-dir
                          :project/version}
                   :opt #{:jar/main-ns
                          :jar.manifest/overrides
                          :project/author
                          :project.deps/aliases},})


(defn make-jar&clean!
  "Create a jar, simplifying the process by handling the creation and deletion of the temporary output put that will
  be zipped into the resulting jar."
  [{out :project/output-dir
    jar-out :jar/output
    :as param}]
  (u/ensure-dir! out)
  (fs/delete-if-exists! jar-out)
  (let [res (atom nil)]
    (let [temp-out (fs/create-temp-directory! "temp-out_" {:dir out})]
      (-> param
          (assoc :jar/temp-output temp-out
                 :cleaning/target temp-out)
          (u/side-effect! #(reset! res (mbt-core/jar-add-srcs! %)))
          (u/side-effect! mbt-core/jar-make-archive!)
          (u/side-effect! mbt-core/clean!))
      @res)))

(u/spec-op make-jar&clean!
           :deps [mbt-core/jar-add-srcs!
                  mbt-core/jar-make-archive!
                  mbt-core/clean!]
           :param {:req [:project/working-dir
                         :jar/output
                         :jar/srcs]
                   :opt [:jar/exclude?]})


(defn jar-out
  "Make the jar path given the `:project/output-dir` and `:build/jar-name`."
  [{jar-name :build/jar-name
    out :project/output-dir}]
  (u/safer-path out jar-name))

(u/spec-op jar-out
           :param {:req [:build/jar-name :project/output-dir]}
           :ret :jar/output)


(defn jar!
  "Create a skinny jar. The jar sources are determined using
  [[fr.jeremyschoffen.mbt.alpha.default.jar/simple-jar-srcs]], the jar's path name
  [[fr.jeremyschoffen.mbt.alpha.default.jar/jar-out]].

  This function takes care of generating a deleting a temporary directory used to group the
  jar files that end up compressed into the jar archive."
  [param]
  (-> param
      (u/ensure-computed
        :jar/srcs simple-jar-srcs
        :jar/output jar-out)
      make-jar&clean!))

(u/spec-op jar!
           :deps [jar-out simple-jar-srcs make-jar&clean!]
           :param {:req [:build/jar-name
                         :classpath/index
                         :jar/manifest
                         :maven/artefact-name
                         :maven/group-id
                         :maven/pom
                         :maven/pom-properties
                         :project/deps
                         :project/output-dir
                         :project/working-dir]
                   :opt [:jar/exclude?]})


(defn uberjar-out
  "Make the uberjar path given the `:project/output-dir` and `:build/jar-name`."
  [{jar-name :build/uberjar-name
    out :project/output-dir}]
  (u/safer-path out jar-name))

(u/spec-op uberjar-out
           :param {:req [:build/uberjar-name :project/output-dir]}
           :ret :jar/output)


(defn uberjar!
  "Build an uberjar. The jar sources are determined using
  [[fr.jeremyschoffen.mbt.alpha.default.jar/uber-jar-srcs]], the uberjar's path
  [[fr.jeremyschoffen.mbt.alpha.default.jar/uberjar-out]].

  This function takes care of generating a deleting a temporary directory used to group the
  jar files that end up compressed into the jar archive."
  [param]
  (-> param
      (u/ensure-computed
        :jar/srcs uber-jar-srcs
        :jar/output uberjar-out)
      make-jar&clean!))

(u/spec-op uberjar!
           :deps [uberjar-out uber-jar-srcs make-jar&clean!]
           :param {:req [:build/uberjar-name
                         :classpath/index
                         :jar/manifest
                         :maven/artefact-name
                         :maven/group-id
                         :maven/pom
                         :maven/pom-properties
                         :project/deps
                         :project/output-dir
                         :project/working-dir]
                   :opt [:jar/exclude?]})