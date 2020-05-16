(ns com.jeremyschoffen.mbt.alpha.building.cleaning
  (:require
    [cognitect.anomalies :as anom]
    [com.jeremyschoffen.mbt.alpha.specs]
    [com.jeremyschoffen.java.nio.file :as fs]
    [com.jeremyschoffen.mbt.alpha.utils :as u]))

;; TODO: redo since simply listing might put files in the wrong order for deletion
(defn list-files-recursivelly [dir]
  (-> dir
      fs/walk
      fs/realize))

;; TODO: Rethink the way way allow deletions and maybe test all files before starting to delete.
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
           :param {:req [:project/output-dir
                         :cleaning/files-to-delete]})