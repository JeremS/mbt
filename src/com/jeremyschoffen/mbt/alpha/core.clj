(ns com.jeremyschoffen.mbt.alpha.core
  (:require
    [com.jeremyschoffen.mbt.alpha.core.building.classpath :as classpath]
    [com.jeremyschoffen.mbt.alpha.core.building.cleaning :as cleaning]
    [com.jeremyschoffen.mbt.alpha.core.building.compilation :as compilation]
    [com.jeremyschoffen.mbt.alpha.core.building.deps :as deps]
    [com.jeremyschoffen.mbt.alpha.core.building.gpg :as gpg]
    [com.jeremyschoffen.mbt.alpha.core.building.jar :as jar]
    [com.jeremyschoffen.mbt.alpha.core.building.manifest :as manifest]
    [com.jeremyschoffen.mbt.alpha.core.building.maven.common :as maven-common]
    [com.jeremyschoffen.mbt.alpha.core.building.maven.deploy :as deploy]
    [com.jeremyschoffen.mbt.alpha.core.building.maven.install :as install]
    [com.jeremyschoffen.mbt.alpha.core.building.maven.pom :as pom]
    [com.jeremyschoffen.mbt.alpha.core.versioning.maven-like :as maven-like]
    [com.jeremyschoffen.mbt.alpha.core.versioning.simple-version :as simple-version]
    [com.jeremyschoffen.mbt.alpha.core.utils :as u]))



(u/alias-fn raw-classpath classpath/raw-classpath)
(u/alias-fn indexed-classpath classpath/indexed-classpath)

(u/alias-fn clean! cleaning/clean!)

(u/alias-fn compile! compilation/compile!)

(u/alias-fn get-deps deps/get-deps)

(u/alias-fn sign-file! gpg/sign-file!)
(u/alias-fn sign-files! gpg/sign-files!)

(u/alias-fn add-srcs! jar/add-srcs!)
(u/alias-fn simple-jar-srcs jar/simple-jar-srcs)
(u/alias-fn uber-jar-srcs jar/uber-jar-srcs)
(u/alias-fn make-jar-archive! jar/make-jar-archive!)

(u/alias-fn make-manifest manifest/make-manifest)

(u/alias-fn make-maven-artefact maven-common/make-maven-artefact)
(u/alias-fn make-maven-artefacts maven-common/make-maven-artefacts)
(u/alias-fn sign-artefact! maven-common/sign-artefact!)
(u/alias-fn sign-artefacts! maven-common/sign-artefacts!)
(u/alias-fn make-usual-artefacts maven-common/make-usual-artefacts)
(u/alias-fn make-usual-artefacts+signatures! maven-common/make-usual-artefacts+signatures!)

(u/alias-fn deploy! deploy/deploy!)

(u/alias-fn install! install/install!)

(u/alias-fn new-pom pom/new-pom)
(u/alias-fn sync-pom! pom/sync-pom!)

(u/alias-fn parse-maven-version maven-like/parse-version)
(u/alias-fn parse-semver-version maven-like/parse-version)

(u/alias-fn maven-version maven-like/maven-version)
(u/alias-fn semver-version maven-like/semver-version)

(u/alias-fn maven-bump maven-like/safer-bump)
(u/alias-fn semver-bump maven-like/safer-bump)

(u/alias-fn parse-simple-version simple-version/parse-version-number)
(u/alias-fn simple-version simple-version/simple-version)
(u/alias-fn simple-version-bump simple-version/bump)
