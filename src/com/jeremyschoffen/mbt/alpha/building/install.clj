(ns com.jeremyschoffen.mbt.alpha.building.install
  (:require
    [clojure.tools.deps.alpha.util.maven :as maven]
    [clojure.java.io :as io]
    [com.jeremyschoffen.java.nio.file :as fs]
    [com.jeremyschoffen.mbt.alpha.specs]
    [com.jeremyschoffen.mbt.alpha.utils :as u])
  (:import
    [org.eclipse.aether.installation InstallRequest]))

;;TODO: add the notion of classifier
;;TODO: maybe skip the use of maven/coord->artefact
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
           :param {:req [:artefact/name
                         :maven/group-id
                         :project/version
                         :jar/output
                         :maven.pom/dir]
                   :opt [:maven/local-repo]})
