(ns com.jeremyschoffen.mbt.alpha.building.cleaning
  (:require
    [clojure.spec.alpha :as s]
    [cognitect.anomalies :as anom]
    [com.jeremyschoffen.mbt.alpha.specs]
    [com.jeremyschoffen.java.nio.file :as fs]
    [com.jeremyschoffen.mbt.alpha.utils :as u]))


(defn list-files-recursivelly [dir]
  (-> dir
      fs/walk
      fs/realize))

(u/spec-op list-files-recursivelly
           any?)



(defn delete-files! [{output-dir :project/output-dir
                      files :cleaning/files-to-delete}]
  (doseq [f files]
    (when-not (fs/ancestor? output-dir f)
      (throw (ex-info "Can't delete outside of the output-dir"
                      {::anom/category ::anom/forbidden
                       :mbt/error :deletion-outside-output-dir
                       :file f
                       :output-dir output-dir})))
    (fs/delete! f)))

(u/spec-op delete-files!
           (s/keys :req [:project/output-dir
                         :cleaning/files-to-delete]))