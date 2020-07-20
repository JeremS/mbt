(ns project1.core
  (:require [project2.core :as p2]))


(defn call-project2 [] (p2/say-hello))