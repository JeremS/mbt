(ns com.jeremyschoffen.mbt.alpha.core
  (:require
    [com.jeremyschoffen.mbt.alpha.core.building.classpath :as classpath]
    [com.jeremyschoffen.mbt.alpha.core.building.cleaning :as cleaning]
    [com.jeremyschoffen.mbt.alpha.core.building.compilation :as compilation]
    [com.jeremyschoffen.mbt.alpha.core.building.deps :as deps]
    [com.jeremyschoffen.mbt.alpha.core.gpg :as gpg]
    [com.jeremyschoffen.mbt.alpha.core.building.jar :as jar]
    [com.jeremyschoffen.mbt.alpha.core.building.manifest :as manifest]
    [com.jeremyschoffen.mbt.alpha.core.specs]
    [com.jeremyschoffen.mbt.alpha.core.git :as git]
    [com.jeremyschoffen.mbt.alpha.core.maven.common :as maven-common]
    [com.jeremyschoffen.mbt.alpha.core.maven.deploy :as deploy]
    [com.jeremyschoffen.mbt.alpha.core.maven.install :as install]
    [com.jeremyschoffen.mbt.alpha.core.maven.pom :as pom]
    [com.jeremyschoffen.mbt.alpha.core.versioning.maven-like :as maven-like]
    [com.jeremyschoffen.mbt.alpha.core.versioning.simple-version :as simple-version]
    [com.jeremyschoffen.mbt.alpha.utils :as u]))


;;----------------------------------------------------------------------------------------------------------------------
;; Classpath
;;----------------------------------------------------------------------------------------------------------------------
(u/alias-fn classpath-raw classpath/raw-classpath)
(u/alias-fn classpath-indexed classpath/indexed-classpath)

;;----------------------------------------------------------------------------------------------------------------------
;; Cleaning
;;----------------------------------------------------------------------------------------------------------------------
(u/alias-fn clean! cleaning/clean!)

;;----------------------------------------------------------------------------------------------------------------------
;; Compilation
;;----------------------------------------------------------------------------------------------------------------------
(u/alias-fn compilation-project-nss compilation/project-nss)
(u/alias-fn compilation-external-nss compilation/external-nss)
(u/alias-fn compilation-jar-nss compilation/jar-nss)
(u/alias-fn compile! compilation/compile!)

;;----------------------------------------------------------------------------------------------------------------------
;; Deps
;;----------------------------------------------------------------------------------------------------------------------
(u/alias-fn deps-get deps/get-deps)
(u/alias-fn deps-make-coord deps/make-deps-coords)

;;----------------------------------------------------------------------------------------------------------------------
;; GPG
;;----------------------------------------------------------------------------------------------------------------------
(u/alias-fn gpg-version gpg/gpg-version)
(u/alias-fn gpg-sign-file! gpg/sign-file!)

;;----------------------------------------------------------------------------------------------------------------------
;; Jar
;;----------------------------------------------------------------------------------------------------------------------
(u/alias-fn jar-open-fs jar/open-jar-fs)
(u/alias-fn jar-add-srcs! jar/add-srcs!)
(u/alias-fn jar-make-archive! jar/make-jar-archive!)

(u/alias-fn manifest manifest/make-manifest)

;;----------------------------------------------------------------------------------------------------------------------
;; Maven
;;----------------------------------------------------------------------------------------------------------------------
(u/alias-fn maven-new-pom pom/new-pom)
(u/alias-fn maven-sync-pom! pom/sync-pom!)

(u/alias-def maven-default-local-repo maven-common/default-local-repo)
(u/alias-def maven-default-settings-file maven-common/maven-default-settings-file)

(u/alias-fn maven-sign-artefact! maven-common/sign-artefact!)
(u/alias-fn maven-sign-artefacts! maven-common/sign-artefacts!)

(u/alias-fn maven-deploy! deploy/deploy!)
(u/alias-fn maven-install! install/install!)

;;----------------------------------------------------------------------------------------------------------------------
;; Git
;;----------------------------------------------------------------------------------------------------------------------
(u/alias-fn git-top-level git/top-level)
(u/alias-fn git-prefix git/prefix)
(u/alias-fn git-make-jgit-repo git/make-jgit-repo)
(u/alias-fn git-status git/status)
(u/alias-fn git-add! git/add!)
(u/alias-fn git-add-all! git/add-all!)
(u/alias-fn git-update-all! git/update-all!)
(u/alias-fn git-list-all-changed-patterns git/list-all-changed-patterns)
(u/alias-fn git-commit! git/commit!)
(u/alias-fn git-git-get-tag git/get-tag)
(u/alias-fn git-tag! git/tag!)
(u/alias-fn git-dirty? git/dirty?)
(u/alias-fn git-describe-raw git/describe-raw)
(u/alias-fn git-describe git/describe)
(u/alias-fn git-any-commit? git/any-commit?)


;;----------------------------------------------------------------------------------------------------------------------
;; Versioning
;;----------------------------------------------------------------------------------------------------------------------
(u/alias-fn version-parse-maven-like maven-like/parse-version)

;;----------------------------------------------------------------------------------------------------------------------
;; Maven
(u/alias-fn version-maven maven-like/maven-version)
(u/alias-def version-initial-maven maven-like/initial-maven-version)
(u/alias-fn version-maven-bump maven-like/safer-bump)

;;----------------------------------------------------------------------------------------------------------------------
;; Semver
(u/alias-fn semver-version maven-like/semver-version)
(u/alias-def version-initial-semver maven-like/initial-semver-version)
(u/alias-fn version-semver-bump maven-like/safer-bump)

;;----------------------------------------------------------------------------------------------------------------------
;; Simple
(u/alias-fn version-parse-simple simple-version/parse-version-number)
(u/alias-fn version-simple simple-version/simple-version)
(u/alias-def version-initial-simple simple-version/initial-simple-version)
(u/alias-fn version-simple-bump simple-version/bump)
