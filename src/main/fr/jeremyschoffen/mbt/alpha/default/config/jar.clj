(ns fr.jeremyschoffen.mbt.alpha.default.config.jar
  (:require
    [fr.jeremyschoffen.mbt.alpha.default.specs]
    [fr.jeremyschoffen.mbt.alpha.default.config.impl :as impl]
    [fr.jeremyschoffen.mbt.alpha.utils :as u]))

(u/pseudo-nss
  build
  build.jar
  maven
  project)


(defn jar-out-dir [{general-out ::project/output-dir}]
  general-out)

(u/spec-op jar-out-dir
           :param {:req [::project/output-dir]}
           :ret ::build.jar/output-dir)


(defn jar-name
  "\"artefact-name.jar\""
  [{artefact-name ::maven/artefact-name}]
  (str artefact-name ".jar"))

(u/spec-op jar-name
           :param {:req [::maven/artefact-name]}
           :ret ::build/jar-name)


(defn uberjar-name
  "\"artefact-name-standalone.jar\""
  [{artefact-name ::maven/artefact-name}]
  (str artefact-name "-standalone.jar"))

(u/spec-op uberjar-name
           :param {:req [::maven/artefact-name]}
           :ret ::build/uberjar-name)


(def conf {::build.jar/output-dir (impl/calc jar-out-dir ::project/output-dir)
           ::build/jar-name (impl/calc jar-name ::maven/artefact-name)
           ::build/uberjar-name (impl/calc uberjar-name ::maven/artefact-name)})
