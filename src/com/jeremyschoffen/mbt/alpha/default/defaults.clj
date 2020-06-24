(ns com.jeremyschoffen.mbt.alpha.default.names
  (:require
    [clojure.string :as string]
    [com.jeremyschoffen.java.nio.file :as fs]
    [com.jeremyschoffen.mbt.alpha.core.git :as git]
    [com.jeremyschoffen.mbt.alpha.core.specs]
    [com.jeremyschoffen.mbt.alpha.default.specs]
    [com.jeremyschoffen.mbt.alpha.core.utils :as u]))


(defn group-id [param]
  (-> param
      git/top-level
      fs/file-name
      str
      symbol))

(u/spec-op group-id
           :deps [git/top-level]
           :param {:req [:project/working-dir]}
           :ret :maven/group-id)


(defn artefact-name [param]
  (let [prefix (git/prefix param)]
    (if-not (-> prefix str seq)
      (group-id param)
      (->> prefix
           (map str)
           (string/join "-")
           symbol))))

(u/spec-op artefact-name
           :deps [group-id git/prefix git/top-level]
           :param {:req [:project/working-dir]}
           :ret :maven/artefact-name)


(defn tag-base-name [param]
  (-> param
      artefact-name
      str))

(u/spec-op tag-base-name
           :deps [artefact-name]
           :param {:req [:project/working-dir]}
           :ret :versioning/tag-base-name)


;; TODO: make functions that name jars