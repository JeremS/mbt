(ns fr.jeremyschoffen.mbt.alpha.default.config.compilation
  (:require
    [fr.jeremyschoffen.java.nio.alpha.file :as fs]
    [fr.jeremyschoffen.mbt.alpha.default.specs]
    [fr.jeremyschoffen.mbt.alpha.default.config.impl :as impl]
    [fr.jeremyschoffen.mbt.alpha.utils :as u]))

(u/pseudo-nss
  compilation.clojure
  compilation.java
  project)

;;----------------------------------------------------------------------------------------------------------------------
;; Compilation
;;----------------------------------------------------------------------------------------------------------------------
(defn compilation-clojure-dir
  "The default clojure compilation directory: \"output-dir/classes\""
  [{out ::project/output-dir}]
  (fs/path out "classes"))

(u/spec-op compilation-clojure-dir
           :param {:req [::project/output-dir]}
           :ret ::compilation.clojure/output-dir)


(defn compilation-java-dir
  "The default java compilation directory: \"output-dir/classes\""
  [{out ::project/output-dir}]
  (fs/path out "classes"))

(u/spec-op compilation-java-dir
           :param {:req [::project/output-dir]}
           :ret ::compilation.java/output-dir)


(def conf {::compilation.clojure/output-dir (impl/calc compilation-clojure-dir ::project/output-dir)
           ::compilation.java/output-dir (impl/calc compilation-clojure-dir ::project/output-dir)})
