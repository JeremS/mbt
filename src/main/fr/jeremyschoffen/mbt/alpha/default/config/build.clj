(ns ^{:author "Jeremy Schoffen"
      :doc "
Default config pertaining to building utilities.
      "}
  fr.jeremyschoffen.mbt.alpha.default.config.build
  (:require
    [fr.jeremyschoffen.mbt.alpha.default.specs]
    [fr.jeremyschoffen.mbt.alpha.default.config.impl :as impl]
    [fr.jeremyschoffen.mbt.alpha.utils :as u]))

(u/pseudo-nss
  build
  build.jar
  build.uberjar
  maven
  project)


(defn jar-out-dir
  "Returns `...mbt.alpha.project/output-dir`"
  [{general-out ::project/output-dir}]
  general-out)

(u/spec-op jar-out-dir
           :param {:req [::project/output-dir]}
           :ret ::build/jar-output-dir)


(defn jar-name
  "Make the default name of skinny jars."
  [{artefact-name ::maven/artefact-name}]
  (str artefact-name ".jar"))

(u/spec-op jar-name
           :param {:req [::maven/artefact-name]}
           :ret ::build.jar/name)


(defn jar-out
  "Make the path at which skinny jar are built by default."
  [{jar-name ::build.jar/name
    out ::build/jar-output-dir}]
  (u/safer-path out jar-name))

(u/spec-op jar-out
           :param {:req [::build/jar-name
                         ::build.jar/output-dir]}
           :ret ::build.jar/path)


(defn uberjar-name
  "Make the default name of skinny jars."
  [{artefact-name ::maven/artefact-name}]
  (str artefact-name "-standalone.jar"))

(u/spec-op uberjar-name
           :param {:req [::maven/artefact-name]}
           :ret ::build.uberjar/name)


(defn uberjar-out
  "Make path at which uberjar are built by default."
  [{jar-name ::build.uberjar/name
    out ::build/jar-output-dir}]
  (u/safer-path out jar-name))

(u/spec-op uberjar-out
           :param {:req [::build/uberjar-name
                         ::build.jar/output-dir]}
           :ret ::build.uberjar/path)


(def conf {::build.jar/allow-non-maven-deps false
           ::build/jar-output-dir (impl/calc jar-out-dir ::project/output-dir)

           ::build.jar/name (impl/calc jar-name ::maven/artefact-name)
           ::build.jar/path (impl/calc jar-out ::build/jar-output-dir ::build.jar/name)

           ::build.uberjar/name (impl/calc uberjar-name ::maven/artefact-name)
           ::build.uberjar/path (impl/calc  uberjar-out ::build/jar-output-dir ::build.uberjar/name)})
