(ns ^{:author "Jeremy Schoffen"
      :doc "
Api grouping the different utilities around building jars provided in
`fr.jeremyschoffen.mbt.alpha.core.jar.XXX` namespaces.
      "}
  fr.jeremyschoffen.mbt.alpha.core.jar
  (:require
    [fr.jeremyschoffen.mbt.alpha.core.specs]
    [fr.jeremyschoffen.mbt.alpha.core.jar.archive :as archive]
    [fr.jeremyschoffen.mbt.alpha.core.jar.fs :as jar-fs]
    [fr.jeremyschoffen.mbt.alpha.core.jar.protocols :as p]
    [fr.jeremyschoffen.mbt.alpha.core.jar.sources]
    [fr.jeremyschoffen.mbt.alpha.core.jar.temp :as temp]

    [fr.jeremyschoffen.mbt.alpha.utils :as u]))


(u/def-clone read-only-jar-fs jar-fs/read-only-jar-fs)
(u/def-clone writable-jar-fs jar-fs/writable-jar-fs)

(u/def-clone to-entries p/to-entries)

(u/def-clone add-entry! temp/add-entry!)
(u/def-clone add-entries! temp/add-entries!)
(u/def-clone add-srcs! temp/add-srcs!)


(u/def-clone make-jar-archive! archive/make-jar-archive!)






