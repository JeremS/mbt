(ns com.jeremyschoffen.mbt.alpha.default
  (:require
    [com.jeremyschoffen.mbt.alpha.default.building :as building]
    [com.jeremyschoffen.mbt.alpha.default.defaults :as defaults]
    [com.jeremyschoffen.mbt.alpha.default.specs]
    [com.jeremyschoffen.mbt.alpha.default.tasks :as tasks]
    [com.jeremyschoffen.mbt.alpha.default.versioning :as versioning]
    [com.jeremyschoffen.mbt.alpha.utils :as u]))

;;----------------------------------------------------------------------------------------------------------------------
;; Default conf
;;----------------------------------------------------------------------------------------------------------------------
(u/alias-fn make-conf defaults/make-context)

;;----------------------------------------------------------------------------------------------------------------------
;; Versioning schemes
;;----------------------------------------------------------------------------------------------------------------------
(def maven-scheme versioning/maven-scheme)
(def semver-scheme versioning/semver-scheme)
(def simple-scheme versioning/simple-scheme)


;;----------------------------------------------------------------------------------------------------------------------
;; Versioning machinery
;;----------------------------------------------------------------------------------------------------------------------
;; Git versioning
(u/alias-fn bump-tag! versioning/bump-tag!)

;;----------------------------------------------------------------------------------------------------------------------
;; Premade
;;----------------------------------------------------------------------------------------------------------------------
(u/alias-fn add-version-file! tasks/add-version-file!)
(u/alias-fn build-jar! tasks/jar!)
(u/alias-fn build-uberjar! tasks/uberjar!)