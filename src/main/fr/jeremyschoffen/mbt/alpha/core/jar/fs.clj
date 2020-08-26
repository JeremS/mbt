(ns ^{:author "Jeremy Schoffen"
      :doc "
Api providing some jar file system utilies.
      "}
  fr.jeremyschoffen.mbt.alpha.core.jar.fs
  (:require
    [fr.jeremyschoffen.java.nio.alpha.file :as fs]
    [fr.jeremyschoffen.mbt.alpha.core.specs :as specs]
    [fr.jeremyschoffen.mbt.alpha.utils :as u])
  (:import
    (java.nio.file FileSystem)
    (java.net URI)
    (java.util HashMap)))

(u/mbt-alpha-pseudo-nss
  jar)
;;----------------------------------------------------------------------------------------------------------------------
;; Jar FileSystem cstr
;;----------------------------------------------------------------------------------------------------------------------
(defn- jar-path->uri
  {:tag URI}
  [jar-path]
  (-> jar-path
      fs/uri
      (->> (str "jar:"))
      fs/uri))


(defn- jar-create-env []
  (doto (HashMap.)
    (.put "create" "true")
    (.put "encoding" "UTF-8")))


(defn writable-jar-fs
  "Create a jar file system located at the path specified under the key `:jar/output`.
  This file system is created with the purpose of the creation of a fresh jar in mind."
  {:tag FileSystem}
  [{output ::jar/output}]
  (u/ensure-parent! output)
  (-> output
      jar-path->uri
      (fs/file-system (jar-create-env))))

(u/spec-op writable-jar-fs
           :param {:req [::jar/output]}
           :ret ::jar/file-system)


(defn- jar-read-env []
  (doto (HashMap.)
    (.put "encoding" "UTF-8")))


(defn read-only-jar-fs
  "Open a jar (zip) file system at the location passed as a parameter.
  This file system is read only."
  {:tag FileSystem}
  [jar-path]
  (-> jar-path
      jar-path->uri
      (fs/file-system (jar-read-env))))

(u/simple-fdef read-only-jar-fs
               specs/jar-path?
               ::jar/file-system)
