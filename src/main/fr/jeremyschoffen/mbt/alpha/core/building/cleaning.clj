(ns fr.jeremyschoffen.mbt.alpha.core.building.cleaning
  (:require
    [cognitect.anomalies :as anom]
    [fr.jeremyschoffen.java.nio.alpha.file :as fs]
    [fr.jeremyschoffen.mbt.alpha.core.specs]
    [fr.jeremyschoffen.mbt.alpha.utils :as u])
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

(defn- check-wd [{wd :project/working-dir
                  :as param}]
  (when-not wd
    (throw (ex-info "Can't clean without a project directory for reference."
                    (assoc param
                      ::anom/category ::anom/forbidden
                      :mbt/error :no-project-directory)))))

(u/spec-op check-wd
           :param {:req [:project/working-dir]})


(defn- check-proper-ancestry [{wd :project/working-dir
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


(defn clean!
  "Delete a file or directory at the path specified by the key `:cleaning/target`. The target can't be the project
  directory specified by the key `:project/working-dir` and must be inside it. You can still delete anything inside
  your project dir with this function, src dir included.

  Returns the cleaning target."
  [{t :cleaning/target
    :as param}]
  (when-not (fs/exists? t)
    (throw (ex-info "File to clean doesn't exist."
                    (merge param
                           {::anom/category ::anom/not-found}))))
  (-> param
      (u/check check-wd)
      (u/check check-proper-ancestry)
      clean!*)
  t)

(u/spec-op clean!
           :deps [check-wd check-proper-ancestry]
           :param {:req [:cleaning/target
                         :project/working-dir]})
