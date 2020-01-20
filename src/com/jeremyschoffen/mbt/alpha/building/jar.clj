(ns com.jeremyschoffen.mbt.alpha.building.jar
  (:require
    [clojure.data.xml :as xml]
    [clojure.spec.alpha :as s]
    [clojure.string :as string]
    [com.jeremyschoffen.java.nio.file :as fs]
    [com.jeremyschoffen.mbt.alpha.specs]
    [com.jeremyschoffen.mbt.alpha.building.pom :as pom]
    [com.jeremyschoffen.mbt.alpha.utils :as u])
  (:import
    (java.nio.file FileSystem FileSystems)
    (java.net URI)
    (java.util HashMap)))

;;----------------------------------------------------------------------------------------------------------------------
;; Utils
;;----------------------------------------------------------------------------------------------------------------------
(defn ensure-dir! [d]
  (when (fs/not-exists? (fs/create-directories! d)))
  d)


(defn ensure-parent! [f]
  (-> f fs/parent ensure-dir!)
  f)

;;----------------------------------------------------------------------------------------------------------------------
;; Jar FileSystem cstr
;;----------------------------------------------------------------------------------------------------------------------
(defn- output->uri [o]
  (-> o
      fs/uri
      (->> (str "jar:"))
      fs/uri))


(defn- ^FileSystem make-file-system [^URI uri]
  (fs/file-system uri (doto (HashMap.)
                        (.put "create" "true")
                        (.put "encoding" "UTF-8"))))


(defn ^FileSystem make-jar-fs [{output :jar/output}]
  (ensure-parent! output)
  (-> output output->uri make-file-system))

(u/spec-op make-jar-fs
           (s/keys :req [:jar/output])
           :jar/file-system)


;;----------------------------------------------------------------------------------------------------------------------
;; Manifest
;;----------------------------------------------------------------------------------------------------------------------
(def meta-dir "META-INF")
(def manifest-name "MANIFEST.MF")
(def manifest-path (fs/path meta-dir manifest-name))


(defn- str->input-stream [^String s]
  (-> s .getBytes fs/input-stream))


(defn add-manifest! [{manifest :jar/manifest
                      zfs :jar/file-system}]
  (let [dest (ensure-parent! (u/safer-path zfs (str manifest-path)))]
    (fs/copy! (str->input-stream manifest) dest)))

(u/spec-op add-manifest!
           (s/keys :req [:jar/file-system
                         :jar/manifest]))

;;----------------------------------------------------------------------------------------------------------------------
;; Pom
;;----------------------------------------------------------------------------------------------------------------------
(def maven-dir "maven")


(defn- make-jar-maven-path [zfs group-id artefact-id]
  (fs/path zfs meta-dir maven-dir (str group-id) (str artefact-id)))


(defn add-pom! [{zfs :jar/file-system
                 pom :maven/pom
                 group-id :maven/group-id
                 artefact-id :artefact/name}]
  (let [pom-target-dir (ensure-dir! (make-jar-maven-path zfs group-id artefact-id))
        src (-> pom xml/indent-str str->input-stream)
        target (pom/pom-path pom-target-dir)]
    (fs/copy! src target)))

(u/spec-op add-pom!
           (s/keys :req [:jar/file-system
                         :maven/pom
                         :maven/group-id
                         :artefact/name]))


(comment
  (fs/delete-if-exists! (u/safer-path "target/mbt.jar"))
  (require '[com.jeremyschoffen.mbt.alpha.building.manifest :as manifest])

  (-> {:jar/output (u/safer-path "target/mbt.jar")
       :jar/manifest (manifest/make-manifest {})
       :maven.pom/dir "."
       :maven/group-id 'super
       :artefact/name "project"}
      (u/assoc-computed :maven/pom pom/new-pom)
      (u/assoc-computed :jar/file-system make-jar-fs)
      (as-> context
            (with-open [_ (-> context :jar/file-system fs/file-system)]
              (-> context
                  (u/side-effect! add-manifest!)
                  (u/side-effect! add-pom!)))))

  (with-open [zfs (make-jar-fs {:jar/output (u/safer-path "target/mbt.jar")})]
    (fs/realize (fs/walk (fs/path zfs "/")))))