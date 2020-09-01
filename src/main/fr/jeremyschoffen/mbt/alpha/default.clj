(ns ^{:author "Jeremy Schoffen"
      :doc "
Facade grouping the default apis in one place.
      "}
  fr.jeremyschoffen.mbt.alpha.default
  (:require
    [fr.jeremyschoffen.java.nio.alpha.file :as fs]
    [fr.jeremyschoffen.mbt.alpha.core :as mbt-core]
    [fr.jeremyschoffen.mbt.alpha.default.config :as config]
    [fr.jeremyschoffen.mbt.alpha.default.maven :as maven]
    [fr.jeremyschoffen.mbt.alpha.default.specs]
    [fr.jeremyschoffen.mbt.alpha.default.tasks :as tasks]
    [fr.jeremyschoffen.mbt.alpha.default.versioning :as versioning]
    [fr.jeremyschoffen.mbt.alpha.utils :as u]))

(u/pseudo-nss
  maven.server)

;;----------------------------------------------------------------------------------------------------------------------
;; Default conf
;;----------------------------------------------------------------------------------------------------------------------
(u/def-clone config config/make-base-config)
(u/def-clone config-calc config/calc)
(u/def-clone config-compute config/compute-conf)
(u/def-clone config-print-deps config/pprint-deps)


;;----------------------------------------------------------------------------------------------------------------------
;; Versioning schemes
;;----------------------------------------------------------------------------------------------------------------------
(u/def-clone maven-scheme versioning/maven-scheme)
(u/def-clone semver-scheme versioning/semver-scheme)
(u/def-clone git-distance-scheme versioning/git-distance-scheme)


;;----------------------------------------------------------------------------------------------------------------------
;; Versioning machinery
;;----------------------------------------------------------------------------------------------------------------------
(u/def-clone versioning-initial-version versioning/schemes-initial-version)
(u/def-clone versioning-current-version versioning/current-version)
(u/def-clone versioning-next-version versioning/next-version)
(u/def-clone versioning-make-next-version+x versioning/next-version+x)



;; Git versioning
(u/def-clone versioning-tag-new-version! versioning/tag-new-version!)

;;----------------------------------------------------------------------------------------------------------------------
;; Premade
;;----------------------------------------------------------------------------------------------------------------------
(u/def-clone write-version-file! versioning/write-version-file!)


(u/def-clone build-before-bump! tasks/generate-before-bump!)
(u/def-clone build-jar! tasks/jar!)
(u/def-clone build-uberjar! tasks/uberjar!)
(u/def-clone maven-sync-pom! mbt-core/maven-sync-pom!)
(u/def-clone maven-install! maven/install!)
(u/def-clone maven-deploy! maven/deploy!)

;;----------------------------------------------------------------------------------------------------------------------
;; Default remote repo
;;----------------------------------------------------------------------------------------------------------------------
(def clojars
  "Representation of clojars following the `:maven/server` spec."
  {::maven.server/id "clojars"
   ::maven.server/url (fs/url "https://repo.clojars.org/")})
