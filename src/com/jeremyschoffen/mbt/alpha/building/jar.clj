(ns com.jeremyschoffen.mbt.alpha.building.jar
  (:require
    [clojure.data.xml :as xml]
    [clojure.spec.alpha :as s]
    [clojure.string :as string]
    [com.jeremyschoffen.java.nio.file :as fs]
    [com.jeremyschoffen.mbt.alpha.specs]
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

(defn make-manifest-entry [{manifest :jar/manifest}]
  {:jar.entry/src manifest
   :jar.entry/dest manifest-path})

(u/spec-op make-manifest-entry
           (s/keys :req [:jar/manifest]))

;;----------------------------------------------------------------------------------------------------------------------
;; Pom
;;----------------------------------------------------------------------------------------------------------------------
(def maven-dir "maven")


(defn- make-jar-maven-path [group-id artefact-id]
  (fs/path meta-dir maven-dir (str group-id) (str artefact-id) "pom.xml"))

(defn make-pom-entry [{pom :maven/pom
                       group-id :maven/group-id
                       artefact-id :artefact/name}]
  {:jar.entry/src (xml/indent-str pom)
   :jar.entry/dest (make-jar-maven-path group-id artefact-id)})

(u/spec-op make-pom-entry
           (s/keys :req [:maven/pom
                         :maven/group-id
                         :artefact/name]))

;;----------------------------------------------------------------------------------------------------------------------
;; Jar
;;----------------------------------------------------------------------------------------------------------------------
(defn- str->input-stream [^String s]
  (-> s .getBytes fs/input-stream))

(defn jar! [{zfs :jar/file-system
             entries :jar/entries}]
  (doseq [entry entries]
    (let [{:jar.entry/keys [src dest]} entry
          src (cond-> src
                      (string? src) str->input-stream)
          dest (apply fs/path zfs (map str dest))]
      (ensure-parent! dest)
      (fs/copy! src dest))))

(u/spec-op jar!
           (s/keys :req [:jar/file-system
                         :jar/entries]))


(comment
  (fs/delete-if-exists! (u/safer-path "target/mbt.jar"))
  (require '[com.jeremyschoffen.mbt.alpha.building.manifest :as manifest])
  (require '[com.jeremyschoffen.mbt.alpha.building.pom :as pom])

  (def context {:project/version "5.5.5"
                :artefact/name "mbt"
                :maven/group-id 'mbt
                :maven.pom/dir "."
                :jar/output (u/safer-path "target/mbt.jar")})


  (-> context
      (assoc :jar/entries [(make-manifest-entry {:jar/manifest (manifest/make-manifest context)})
                           (make-pom-entry (assoc context :maven/pom (pom/new-pom context)))])
      (u/assoc-computed :jar/file-system make-jar-fs)
      (as-> ctxt
            (with-open [_ (-> ctxt :jar/file-system fs/file-system)]
              (jar! ctxt))))

  (with-open [zfs (make-jar-fs {:jar/output (u/safer-path "target/mbt.jar")})]
    (fs/realize (fs/walk (fs/path zfs "/")))))