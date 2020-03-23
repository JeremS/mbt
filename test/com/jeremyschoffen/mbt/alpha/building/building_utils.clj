(ns com.jeremyschoffen.mbt.alpha.building.building-utils
  (:require
    [clojure.tools.deps.alpha.reader :as deps-reader]
    [com.jeremyschoffen.java.nio.file :as fs]
    [com.jeremyschoffen.mbt.alpha.utils :as u]))


(def project1 (fs/path "project1"))
(def project2 (fs/path "project2"))

(def resources-test-dir (u/safer-path "resources-test"))


(defn test-resource-path [x]
  (fs/path resources-test-dir x))


(defn make-project-tree [root-path]
  {(u/safer-path root-path project1 "src" "project1" "core.clj") (slurp (test-resource-path "core_p1.clj"))
   (u/safer-path root-path project1 "src" "data_readers.cljc") (slurp (test-resource-path "data_readers-p1.cljc"))
   (u/safer-path root-path project1 "deps.edn") (-> "deps-p1.edn"
                                                    test-resource-path
                                                    deps-reader/slurp-deps
                                                    (assoc-in [:deps 'project2/project2 :local/root]
                                                              (str (fs/path root-path project2)))
                                                    pr-str)

   (u/safer-path root-path project2 "src" "project2" "core.clj") (slurp (test-resource-path "core_p2.clj"))
   (u/safer-path root-path project2 "src" "data_readers.cljc") (slurp (test-resource-path "data_readers-p2.cljc"))
   (u/safer-path root-path project2 "deps.edn") (slurp (test-resource-path "deps-p2.edn"))})


(defn make-test-project []
  (let [temp-dir (u/safer-path (fs/create-temp-directory! "repo-test"))
        file-tree (make-project-tree temp-dir)]
    (doseq [[dest content] file-tree]
      (-> dest fs/parent fs/create-directories!)
      (spit dest content))
    temp-dir))