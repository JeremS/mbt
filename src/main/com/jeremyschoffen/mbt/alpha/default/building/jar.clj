(ns com.jeremyschoffen.mbt.alpha.default.building.jar
  (:require
    [clojure.data.xml :as xml]
    [com.jeremyschoffen.java.nio.alpha.file :as fs]
    [com.jeremyschoffen.mbt.alpha.core.specs]
    [com.jeremyschoffen.mbt.alpha.utils :as u]))


;;----------------------------------------------------------------------------------------------------------------------
;; Jar srcs construction
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


;; Pom
(def maven-dir "maven")


(defn- make-jar-maven-path [group-id artefact-id]
  (fs/path meta-dir maven-dir (str group-id) (str artefact-id) "pom.xml"))


(defn make-pom-entry
  "Make a `:jar/entry` for a `pom.xml` file."
  [{pom :maven/pom
    group-id :maven/group-id
    artefact-id :maven/artefact-name}]
  {:jar.entry/src (xml/indent-str pom)
   :jar.entry/dest (make-jar-maven-path group-id artefact-id)})

(u/spec-op make-pom-entry
           :param {:req [:maven/pom
                         :maven/group-id
                         :maven/artefact-name]})

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


;; Pom + manifest + deps
(defn make-staples-entries
  "Make a `:jar/src` containing the usual manifest, pom.xml and deps.edn `jar/entry`s."
  [param]
  [(-> param make-manifest-entry)
   (-> param make-pom-entry)
   (-> param make-deps-entry)])

(u/spec-op make-staples-entries
           :deps [make-manifest-entry make-pom-entry make-deps-entry]
           :param {:req [:jar/manifest
                         :maven/artefact-name
                         :maven/group-id
                         :maven/pom
                         :project/deps]}
           :ret :jar/src)


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
                         :project/deps]
                   :opt [:project/author
                         :jar/main-ns
                         :jar.manifest/overrides]}
           :ret :jar/srcs)


(defn uber-jar-srcs
  "Make the jar srcs that will go into an uberjar. Similar to [[simple-jar-srcs]] but also adds the other sources
   present in the classpath."
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
                         :project/deps]}
           :ret :jar/srcs)
