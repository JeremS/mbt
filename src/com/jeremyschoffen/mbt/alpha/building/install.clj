(ns com.jeremyschoffen.mbt.alpha.building.install
  (:require
    [clojure.spec.alpha :as s]
    [clojure.tools.deps.alpha.util.maven :as maven]
    [clojure.java.io :as io]
    [com.jeremyschoffen.java.nio.file :as fs]
    [com.jeremyschoffen.mbt.alpha.specs]
    [com.jeremyschoffen.mbt.alpha.utils :as u]
    [com.jeremyschoffen.mbt.alpha.building.pom :as pom])
  (:import
    [org.eclipse.aether.installation InstallRequest]))


;; adapted from https://github.com/EwenG/badigeon/blob/master/src/badigeon/install.clj
(defn install! [{artefact-name   :artefact/name
                 group-id         :maven/group-id
                 version          :project/version
                 jar              :jar/output
                 pom-dir          :maven.pom/dir
                 maven-local-repo :maven/local-repo
                 :or              {maven-local-repo maven/default-local-repo}}]
  (let [lib (symbol (str group-id)  artefact-name)
        maven-coords {:mvn/version (str version)}
        pom-file-path (-> pom-dir (u/safer-path "pom.xml") fs/file)
        system (maven/make-system)
        session (maven/make-session system (str maven-local-repo))
        artifact (maven/coord->artifact lib maven-coords)
        artifact (.setFile artifact (io/file jar))
        pom-artifact (maven/coord->artifact lib (assoc maven-coords :extension "pom"))
        pom-artifact (.setFile pom-artifact  pom-file-path)]
    (.install system session (-> (InstallRequest.)
                                 (.addArtifact artifact)
                                 (.addArtifact pom-artifact)))))


(u/spec-op install!
           (s/keys :req [:artefact/name
                         :maven/group-id
                         :project/version
                         :jar/output
                         :maven.pom/dir]
                   :opt [:maven/local-repo]))

(comment

  (require '[com.jeremyschoffen.mbt.alpha.classic-scheme :as c])
  (require '[com.jeremyschoffen.mbt.alpha.building.classpath :as cp])
  (require '[com.jeremyschoffen.mbt.alpha.building.deps :as deps])
  (require '[com.jeremyschoffen.mbt.alpha.building.jar :as jar])
  (require '[com.jeremyschoffen.mbt.alpha.building.pom :as pom])
  (require '[com.jeremyschoffen.mbt.alpha.version :as v])

  (require '[clojure.spec.test.alpha :as stest])

  (stest/instrument)

  (-> {:project/working-dir (u/safer-path ".")
       :project/version v/version
       :maven/group-id 'mbt
       :maven.pom/dir (u/safer-path ".")
       :jar/temp-output (u/safer-path "target" "temp-jar")
       :jar/output      (u/safer-path "target" "mbt.jar")}

      c/get-state
      (u/assoc-computed :project/deps deps/get-deps)
      (u/assoc-computed :maven/pom pom/new-pom)
      (u/assoc-computed :classpath/index cp/indexed-classpath)
      (u/assoc-computed :jar/srcs jar/simple-jar-srcs)
      (u/side-effect! pom/sync-pom!)
      (u/side-effect! jar/add-srcs!)
      (u/side-effect! jar/jar!))

  (-> {:project/working-dir (u/safer-path ".")
       :project/version v/version
       :maven/group-id 'mbt
       :maven.pom/dir (u/safer-path ".")
       :jar/output (u/safer-path "target" "mbt.jar")
       :maven/local-repo (u/safer-path "target" "local-repo")}
      c/get-state
      install!))