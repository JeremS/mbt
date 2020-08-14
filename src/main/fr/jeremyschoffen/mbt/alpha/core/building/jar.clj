(ns fr.jeremyschoffen.mbt.alpha.core.building.jar
  (:require
    [fr.jeremyschoffen.mbt.alpha.core.specs]
    [fr.jeremyschoffen.mbt.alpha.core.building.jar.archive :as archive]
    [fr.jeremyschoffen.mbt.alpha.core.building.jar.fs :as jar-fs]
    [fr.jeremyschoffen.mbt.alpha.core.building.jar.protocols :as p]
    [fr.jeremyschoffen.mbt.alpha.core.building.jar.sources]
    [fr.jeremyschoffen.mbt.alpha.core.building.jar.temp :as temp]

    [fr.jeremyschoffen.mbt.alpha.utils :as u]))


(u/def-clone open-jar-fs jar-fs/open-jar-fs)
(u/def-clone make-output-jar-fs jar-fs/make-output-jar-fs)

(u/def-clone to-entries p/to-entries)

(u/def-clone add-entry! temp/add-entry!)
(u/def-clone add-entries! temp/add-entries!)
(u/def-clone add-srcs! temp/add-srcs!)


(u/def-clone make-jar-archive! archive/make-jar-archive!)






