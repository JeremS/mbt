(ns com.jeremyschoffen.mbt.alpha.core.building.cleaning
  (:require
    [cognitect.anomalies :as anom]
    [com.jeremyschoffen.java.nio.file :as fs]
    [com.jeremyschoffen.mbt.alpha.core.specs]
    [com.jeremyschoffen.mbt.alpha.core.utils :as u])
  (:import
    [java.nio.file FileVisitResult FileVisitor]))

(defn- make-delete-all-visitor []
  (reify FileVisitor
    (postVisitDirectory [_ dir exception]
      (if (nil? exception)
        (do
          (fs/delete! dir)
          FileVisitResult/CONTINUE)
        (throw exception)))
    (visitFile [_ path attrs]
      (fs/delete! path)
      FileVisitResult/CONTINUE)
    (preVisitDirectory [_ dir attrs]
      FileVisitResult/CONTINUE)
    (visitFileFailed [_ file exception]
      (throw exception))))

(defn check-wd [{wd :project/working-dir
                 :as param}]
  (when-not wd
    (throw (ex-info "Can't clean without a project directory for reference."
                    (assoc param
                      ::anom/category ::anom/forbidden
                      :mbt/error :no-project-directory)))))

(u/spec-op check-wd
           :param {:req [:project/working-dir]})


(defn check-proper-ancestry [{wd :project/working-dir
                              file :cleaning/target
                              :as param}]
  (when-not (fs/ancestor? wd file)
    (throw (ex-info "Can't clean file outside of the project directory"
                    (assoc param
                      ::anom/category ::anom/forbidden
                      :mbt/error :deletion-outside-working-dir))))
  (when (= wd file)
    (throw (ex-info "Can't clean the whole project directory."
                    (assoc param
                      ::anom/category ::anom/forbidden
                      :mbt/error :deleting-project-directory)))))


(u/spec-op check-proper-ancestry
           :param {:req [:project/working-dir
                         :cleaning/target]})


(defn- clean!* [{file :cleaning/target}]
  (fs/walk-file-tree file (make-delete-all-visitor)))

(u/spec-op clean!*
           :param {:req [:cleaning/target]})


(defn clean! [param]
  (-> param
      (u/check check-wd)
      (u/check check-proper-ancestry)
      clean!*))

(u/spec-op clean!
           :deps [check-wd check-proper-ancestry]
           :param {:req [:cleaning/target
                         :project/working-dir]})


(comment
  (def wd (u/safer-path))
  (def target-dir (u/safer-path wd "target"))
  (def dest-file (u/safer-path target-dir "dest-file"))

  (fs/exists? target-dir)
  (fs/create-directory! target-dir)

  (spit dest-file "some content")

  (slurp dest-file)

  (clean! {:project/working-dir target-dir
           :cleaning/target target-dir})

  (fs/ancestor? target-dir target-dir)

  (fs/exists? target-dir)
  (->> target-dir
       fs/walk
       fs/realize
       (map str))
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
                           :cleaning/files-to-delete]}))





(clojure.repl/doc fs/walk-file-tree)