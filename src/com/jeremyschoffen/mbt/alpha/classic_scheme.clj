(ns com.jeremyschoffen.mbt.alpha.classic-scheme
  (:require
    [clojure.spec.alpha :as s]
    [com.jeremyschoffen.java.nio.file :as fs]
    [com.jeremyschoffen.mbt.alpha.versioning.git-state :as gs]
    [com.jeremyschoffen.mbt.alpha.utils :as u]))

;;----------------------------------------------------------------------------------------------------------------------
;; Git versioning
;;----------------------------------------------------------------------------------------------------------------------

(defn project-name [{top-level :git/top-level}]
  (-> top-level
      fs/file-name
      str))

(u/spec-op project-name
           (s/keys :req [:git/top-level]))


(defn module-name [{project-name :project/name
                    prefix :git/prefix}]
  (if prefix
    (->> prefix seq (interpose "-") (apply str))
    project-name))

(u/spec-op module-name
           (s/keys :req [:project/name]
                   :opt [:git/prefix])
           :module/name)


(defn artefact-name [{project-name :project/name
                      module-name :module/name}]
  (if (= project-name module-name)
    project-name
    (str project-name "-" module-name)))

(u/spec-op artefact-name
           (s/keys :req [:project/name :module/name])
           :artefact/name)


(defn- assoc-names [context]
  (u/assoc-computed context
                    :project/name project-name
                    :module/name module-name
                    :artefact/name artefact-name))


(defn project-names [param]
  (-> param
      assoc-names
      (select-keys #{:project/name :module/name :artefact/name})))

(u/spec-op project-names
           (s/keys :req [:git/top-level :git/prefix])
           (s/keys :req [:project/name
                         :module/name
                         :artefact/name]))

(defn get-state [param]
  (-> param
      gs/basic-git-state
      assoc-names))

(u/spec-op get-state
           (s/keys :req [:project/working-dir])
           (s/merge :git/basic-state
                    (s/keys :req [:project/name
                                  :module/name
                                  :artefact/name])))

;;----------------------------------------------------------------------------------------------------------------------
;; Deps
;;----------------------------------------------------------------------------------------------------------------------
