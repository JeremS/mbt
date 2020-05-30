(ns com.jeremyschoffen.mbt.alpha.core.building.install-test
  (:require
    [clojure.test :refer [deftest testing]]
    [clojure.spec.test.alpha :as stest]
    [testit.core :refer :all]
    [com.jeremyschoffen.java.nio.file :as fs]
    [com.jeremyschoffen.mbt.alpha.core.building.building-utils :as bu]
    [com.jeremyschoffen.mbt.alpha.core.building.classpath :as cp]
    [com.jeremyschoffen.mbt.alpha.core.building.deps :as deps]
    [com.jeremyschoffen.mbt.alpha.core.building.install :as install]
    [com.jeremyschoffen.mbt.alpha.core.building.jar :as jar]
    [com.jeremyschoffen.mbt.alpha.core.building.pom :as pom]
    [com.jeremyschoffen.mbt.alpha.core.utils :as u]))


(stest/instrument)


(defn make-temp-jar-out [project-path n]
  (let [p (fs/path project-path "target" n)]
    (fs/create-directories! p)
    p))


(defn make-temp-jar-path [project-path]
  (make-temp-jar-out project-path "temp-jar"))


(defn make-jar-path [project-path]
  (fs/path project-path "target" (-> project-path fs/file-name (str ".jar"))))


(defn make-jar! [ctxt]
  (-> ctxt
      (u/side-effect! jar/add-srcs!)
      (u/side-effect! jar/jar!)))


(defn list-jar-content [jar-path]
  (with-open [zfs (jar/open-jar-fs jar-path)]
    (->> zfs
         fs/walk
         fs/realize
         (into #{} (map str)))))


(deftest test-local-mvn-install
  (let [project (bu/make-test-project)
        project-path (u/safer-path project bu/project2)
        local-repo (u/safer-path project-path "target" "mvn")
        group-id "mbt"
        artefact-name (str (fs/file-name project-path))
        version "0.1.1"
        jar-path (make-jar-path project-path)
        mvn-jar-name (str artefact-name "-" version ".jar")
        mvn-jar-path (u/safer-path local-repo group-id artefact-name version mvn-jar-name)
        ctxt (-> {:project/working-dir project-path
                  :project/version version
                  :maven/group-id (symbol group-id)
                  :maven.pom/dir project-path
                  :maven/local-repo local-repo
                  :artefact/name artefact-name
                  :jar/temp-output (make-temp-jar-path project-path)
                  :jar/output jar-path}
                 (u/assoc-computed :project/deps deps/get-deps)
                 (u/assoc-computed :classpath/index cp/indexed-classpath)
                 (u/assoc-computed :maven/pom pom/new-pom)
                 (u/assoc-computed :jar/srcs jar/simple-jar-srcs))]
    (pom/sync-pom! ctxt)
    (make-jar! ctxt)
    (install/install! ctxt)

    (fact
      (= (list-jar-content mvn-jar-path)
         (list-jar-content jar-path))
      => true)))
