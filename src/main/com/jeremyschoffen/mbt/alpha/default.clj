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
(u/def-clone make-conf defaults/make-context)

;;----------------------------------------------------------------------------------------------------------------------
;; Versioning schemes
;;----------------------------------------------------------------------------------------------------------------------
(u/def-clone maven-scheme versioning/maven-scheme)
(u/def-clone semver-scheme versioning/semver-scheme)
(u/def-clone simple-scheme versioning/simple-scheme)


;;----------------------------------------------------------------------------------------------------------------------
;; Versioning machinery
;;----------------------------------------------------------------------------------------------------------------------
(u/def-clone current-project-version v/current-project-version)

;; Git versioning
(u/def-clone bump-tag! versioning/bump-tag!)

;;----------------------------------------------------------------------------------------------------------------------
;; Premade
;;----------------------------------------------------------------------------------------------------------------------
(u/def-clone anticipated-next-version tasks/anticipated-next-version)
(u/def-clone add-version-file! tasks/add-version-file!)
(u/def-clone build-jar! tasks/jar!)
(u/def-clone build-uberjar! tasks/uberjar!)
(u/def-clone install! maven/install!)
(u/def-clone deploy! maven/deploy!)

;;----------------------------------------------------------------------------------------------------------------------
;; Default remote repo
;;----------------------------------------------------------------------------------------------------------------------
(def clojars
  "Representation of clojars following the `:maven/server` spec."
  #:maven.server{:id "clojars"
                 :url "https://repo.clojars.org/"})
