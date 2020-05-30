(ns com.jeremyschoffen.mbt.alpha.core.classic-scheme
  (:require
    [clojure.spec.alpha :as s]
    [com.jeremyschoffen.java.nio.file :as fs]
    [com.jeremyschoffen.mbt.alpha.core.versioning.git-state :as gs]
    [com.jeremyschoffen.mbt.alpha.core.utils :as u]))

;;----------------------------------------------------------------------------------------------------------------------
;; Git versioning
;;----------------------------------------------------------------------------------------------------------------------

(defn project-name [{top-level :git/top-level}]
  (-> top-level
      fs/file-name
      str))

(u/spec-op project-name
           :param {:req [:git/top-level]})


(defn module-name [{project-name :project/name
                    prefix :git/prefix}]
  (if prefix
    (->> prefix seq (interpose "-") (apply str))
    project-name))

(u/spec-op module-name
           :param {:req [:project/name]
                   :opt [:git/prefix]}
           :ret :module/name)

(defn artefact-name [{project-name :project/name
                      module-name :module/name}]
  (if (= project-name module-name)
    project-name
    (str project-name "-" module-name)))

(u/spec-op artefact-name
           :param {:req [:project/name :module/name]}
           :ret :artefact/name)

(defn- assoc-names [context]
  (u/assoc-computed context
                    :project/name project-name
                    :module/name module-name
                    :artefact/name artefact-name))

(u/spec-op assoc-names
           :deps [project-name module-name artefact-name]
           :param {:req [:git/top-level]
                   :opt [:git/prefix]})


(defn project-names [param]
  (-> param
      assoc-names
      (select-keys #{:project/name :module/name :artefact/name})))

(u/spec-op project-names
           :param {:req [:git/top-level :git/prefix]}
           :ret (s/keys :req [:project/name
                              :module/name
                              :artefact/name]))


(defn get-state [param]
  (-> param
      gs/basic-git-state
      assoc-names))

(u/spec-op get-state
           :deps [gs/basic-git-state assoc-names]
           :param {:req [:project/working-dir]}
           :ret (s/merge :git/basic-state
                         (s/keys :req [:project/name
                                       :module/name
                                       :artefact/name])))