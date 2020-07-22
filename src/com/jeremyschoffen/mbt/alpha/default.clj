(ns com.jeremyschoffen.mbt.alpha.default
  (:require
    [com.jeremyschoffen.mbt.alpha.default.defaults :as defaults]
    [com.jeremyschoffen.mbt.alpha.default.maven :as maven]
    [com.jeremyschoffen.mbt.alpha.default.specs]
    [com.jeremyschoffen.mbt.alpha.default.tasks :as tasks]
    [com.jeremyschoffen.mbt.alpha.default.versioning :as versioning]
    [com.jeremyschoffen.mbt.alpha.utils :as u]
    [com.jeremyschoffen.mbt.alpha.default.versioning :as v]))

;;----------------------------------------------------------------------------------------------------------------------
;; Default conf
;;----------------------------------------------------------------------------------------------------------------------
(u/alias-fn make-conf defaults/make-context)

;;----------------------------------------------------------------------------------------------------------------------
;; Versioning schemes
;;----------------------------------------------------------------------------------------------------------------------
(u/alias-def maven-scheme versioning/maven-scheme)
(u/alias-def semver-scheme versioning/semver-scheme)
(u/alias-def simple-scheme versioning/simple-scheme)


;;----------------------------------------------------------------------------------------------------------------------
;; Versioning machinery
;;----------------------------------------------------------------------------------------------------------------------
(u/alias-fn current-project-version v/current-project-version)

;; Git versioning
(u/alias-fn bump-tag! versioning/bump-tag!)

;;----------------------------------------------------------------------------------------------------------------------
;; Premade
;;----------------------------------------------------------------------------------------------------------------------
(u/alias-fn anticipated-next-version tasks/anticipated-next-version)
(u/alias-fn add-version-file! tasks/add-version-file!)
(u/alias-fn build-jar! tasks/jar!)
(u/alias-fn build-uberjar! tasks/uberjar!)
(u/alias-fn install! maven/install!)
(u/alias-fn deploy! maven/deploy!)

;;----------------------------------------------------------------------------------------------------------------------
;; Default remote repo
;;----------------------------------------------------------------------------------------------------------------------
(def clojars #:maven.server{:id "clojars"
                            :url "https://repo.clojars.org/"})
