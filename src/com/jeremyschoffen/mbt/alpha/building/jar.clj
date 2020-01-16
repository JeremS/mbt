(ns com.jeremyschoffen.mbt.alpha.building.jar
  (:require
    [clojure.spec.alpha :as s]
    [clojure.string :as string]
    [com.jeremyschoffen.java.nio.file :as fs]
    [com.jeremyschoffen.mbt.alpha.specs]
    [com.jeremyschoffen.mbt.alpha.utils :as u])
  (:import
    (java.nio.file FileSystem FileSystems)
    (java.net URI)
    (java.util HashMap)))



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
  (-> output output->uri make-file-system))

(u/spec-op make-jar-fs
           (s/keys :req [:jar/output]))


(defn ^FileSystem make-empty-jar! [{output :jar/output :as param}]
  (-> output fs/parent fs/create-directories!)
  (with-open [_ (make-jar-fs param)]))

(u/spec-op make-empty-jar!
           (s/keys :req [:jar/output]))



(def meta-dir "META-INF")
(def manifest-name "MANIFEST.MF")
(def manifest-path (fs/path meta-dir manifest-name))


(defn- str->input-stream [s]
  (-> s .getBytes fs/input-stream))

(defn ensure-dest! [f]
  (-> f fs/parent fs/create-directories!)
  f)

(defn add-manifest! [{manifest :jar/manifest :as param}]
  (with-open [zfs (make-jar-fs param)]
    (let [dest (ensure-dest! (u/safer-path zfs (str manifest-path)))]
      (fs/copy! (str->input-stream manifest) dest))))


(defn jar! [param]
  (-> param
      make-empty-jar!
      add-manifest!))

(comment
  (require '[com.jeremyschoffen.mbt.alpha.building.manifest :as manifest])
  (make-empty-jar! {:jar/output (u/safer-path "target/mbt.jar")})
  (add-manifest! {:jar/output (u/safer-path "target/mbt.jar")
                  :jar/manifest (manifest/make-manifest {})}))
