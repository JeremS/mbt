(ns fr.jeremyschoffen.mbt.alpha.core.building.jar.protocols
  (:require [fr.jeremyschoffen.mbt.alpha.utils :as u])
  (:import (java.nio.file Path)))


(defprotocol JarSource
  "Protocol used turn data into [[]]"
  :extend-via-metadata true
  (to-entries [this] [this exclude]))


(defprotocol JarEntries
  :extend-via-metadata true
  (add! [this conf]))
