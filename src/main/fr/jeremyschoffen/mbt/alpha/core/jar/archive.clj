(ns ^{:author "Jeremy Schoffen"
      :doc "
Api providing the tools to make a jar archive from a directory.
      "}
  fr.jeremyschoffen.mbt.alpha.core.jar.archive
  (:require
    [fr.jeremyschoffen.java.nio.alpha.file :as fs]

    [fr.jeremyschoffen.mbt.alpha.core.jar.fs :as jar-fs]
    [fr.jeremyschoffen.mbt.alpha.core.specs]
    [fr.jeremyschoffen.mbt.alpha.utils :as u])
  (:import
    (java.nio.file FileVisitResult FileVisitor)))


(defn- make-archive-visitor! [zfs temp-out]
  (reify FileVisitor
    (postVisitDirectory [_ _ exception]
      (when exception (throw exception))
      FileVisitResult/CONTINUE)

    (visitFile [_ src _]
      (let [dest (->> src
                      (fs/relativize temp-out)
                      (fs/path zfs))]
        (fs/copy! src dest)
        FileVisitResult/CONTINUE))

    (preVisitDirectory [_ dir _]
      (let [dest-dir (->> dir
                          (fs/relativize temp-out)
                          (fs/path zfs))]
        (u/ensure-dir! dest-dir)
        FileVisitResult/CONTINUE))

    (visitFileFailed [_ _ exception]
      (throw exception))))


(defn make-jar-archive!
  "Zips the dir specified by under the key `:jar/temp-output` into a .jar archive file at the location provided under
  the key `:jar/output`."
  [{temp :jar/temp-output
    output :jar/output
    :as param}]
  (with-open [zfs (jar-fs/writable-jar-fs param)]
    (fs/walk-file-tree temp (make-archive-visitor! zfs temp)))
  output)


(u/spec-op make-jar-archive!
           :deps [jar-fs/writable-jar-fs]
           :param {:req [:jar/temp-output :jar/output]})

