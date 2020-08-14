(ns ^{:author "Jeremy Schoffen"
      :doc "
Facade grouping the core apis in one place.
      "}
  fr.jeremyschoffen.mbt.alpha.core
  (:require
    [fr.jeremyschoffen.mbt.alpha.core.building.classpath :as classpath]
    [fr.jeremyschoffen.mbt.alpha.core.building.cleaning :as cleaning]
    [fr.jeremyschoffen.mbt.alpha.core.building.compilation :as compilation]
    [fr.jeremyschoffen.mbt.alpha.core.building.deps :as deps]
    [fr.jeremyschoffen.mbt.alpha.core.gpg :as gpg]
    [fr.jeremyschoffen.mbt.alpha.core.building.jar :as jar]
    [fr.jeremyschoffen.mbt.alpha.core.building.manifest :as manifest]
    [fr.jeremyschoffen.mbt.alpha.core.specs]
    [fr.jeremyschoffen.mbt.alpha.core.git :as git]
    [fr.jeremyschoffen.mbt.alpha.core.maven.common :as maven-common]
    [fr.jeremyschoffen.mbt.alpha.core.maven.deploy :as deploy]
    [fr.jeremyschoffen.mbt.alpha.core.maven.install :as install]
    [fr.jeremyschoffen.mbt.alpha.core.maven.pom :as pom]
    [fr.jeremyschoffen.mbt.alpha.core.versioning.maven-like :as maven-like]
    [fr.jeremyschoffen.mbt.alpha.core.versioning.simple-version :as simple-version]
    [fr.jeremyschoffen.mbt.alpha.utils :as u]))


;;----------------------------------------------------------------------------------------------------------------------
;; Classpath
;;----------------------------------------------------------------------------------------------------------------------
(u/def-clone classpath-raw classpath/raw-classpath)
(u/def-clone classpath-indexed classpath/indexed-classpath)

;;----------------------------------------------------------------------------------------------------------------------
;; Cleaning
;;----------------------------------------------------------------------------------------------------------------------
(u/def-clone clean! cleaning/clean!)

;;----------------------------------------------------------------------------------------------------------------------
;; Compilation
;;----------------------------------------------------------------------------------------------------------------------
(u/def-clone compilation-project-nss compilation/project-nss)
(u/def-clone compilation-external-nss compilation/external-nss)
(u/def-clone compilation-jar-nss compilation/jar-nss)
(u/def-clone compile! compilation/compile!)

;;----------------------------------------------------------------------------------------------------------------------
;; Deps
;;----------------------------------------------------------------------------------------------------------------------
(u/def-clone deps-get deps/get-deps)
(u/def-clone deps-make-coord deps/make-deps-coords)

;;----------------------------------------------------------------------------------------------------------------------
;; GPG
;;----------------------------------------------------------------------------------------------------------------------
(u/def-clone gpg-version gpg/gpg-version)
(u/def-clone gpg-sign-file! gpg/sign-file!)

;;----------------------------------------------------------------------------------------------------------------------
;; Jar
;;----------------------------------------------------------------------------------------------------------------------
(u/def-clone jar-open-fs jar/open-jar-fs)
(u/def-clone jar-add-srcs! jar/add-srcs!)
(u/def-clone jar-make-archive! jar/make-jar-archive!)

(u/def-clone manifest manifest/make-manifest)

;;----------------------------------------------------------------------------------------------------------------------
;; Maven
;;----------------------------------------------------------------------------------------------------------------------
(u/def-clone maven-new-pom pom/new-pom)
(u/def-clone maven-sync-pom! pom/sync-pom!)

(u/def-clone maven-default-local-repo maven-common/default-local-repo)
(u/def-clone maven-default-settings-file maven-common/maven-default-settings-file)

(u/def-clone maven-sign-artefact! maven-common/sign-artefact!)
(u/def-clone maven-sign-artefacts! maven-common/sign-artefacts!)

(u/def-clone maven-deploy! deploy/deploy!)
(u/def-clone maven-install! install/install!)

;;----------------------------------------------------------------------------------------------------------------------
;; Git
;;----------------------------------------------------------------------------------------------------------------------
(u/def-clone git-top-level git/top-level)
(u/def-clone git-prefix git/prefix)
(u/def-clone git-make-jgit-repo git/make-jgit-repo)
(u/def-clone git-status git/status)
(u/def-clone git-add! git/add!)
(u/def-clone git-add-all! git/add-all!)
(u/def-clone git-update-all! git/update-all!)
(u/def-clone git-list-all-changed-patterns git/list-all-changed-patterns)
(u/def-clone git-commit! git/commit!)
(u/def-clone git-git-get-tag git/get-tag)
(u/def-clone git-tag! git/tag!)
(u/def-clone git-dirty? git/dirty?)
(u/def-clone git-describe-raw git/describe-raw)
(u/def-clone git-describe git/describe)
(u/def-clone git-any-commit? git/any-commit?)


;;----------------------------------------------------------------------------------------------------------------------
;; Versioning
;;----------------------------------------------------------------------------------------------------------------------
(u/def-clone version-parse-maven-like maven-like/parse-version)

;;----------------------------------------------------------------------------------------------------------------------
;; Maven
(u/def-clone version-maven maven-like/maven-version)
(u/def-clone version-initial-maven maven-like/initial-maven-version)
(u/def-clone version-maven-bump maven-like/safer-bump)

;;----------------------------------------------------------------------------------------------------------------------
;; Semver
(u/def-clone semver-version maven-like/semver-version)
(u/def-clone version-initial-semver maven-like/initial-semver-version)
(u/def-clone version-semver-bump maven-like/safer-bump)

;;----------------------------------------------------------------------------------------------------------------------
;; Simple
(u/def-clone version-parse-simple simple-version/parse-version)
(u/def-clone version-simple simple-version/simple-version)
(u/def-clone version-initial-simple simple-version/initial-simple-version)
(u/def-clone version-simple-bump simple-version/bump)
