(ns com.jeremyschoffen.mbt.alpha.default.building.maven-test
  (:require
    [clojure.test :refer [deftest testing]]
    [clojure.spec.test.alpha :as stest]
    [testit.core :refer :all]
    [com.jeremyschoffen.java.nio.file :as fs]
    [com.jeremyschoffen.mbt.alpha.core :as mbt-core]
    [com.jeremyschoffen.mbt.alpha.default.building.jar :as jar]
    [com.jeremyschoffen.mbt.alpha.test.repos :as test-repos]
    [com.jeremyschoffen.mbt.alpha.utils :as u]))


(stest/instrument)


(def project-path test-repos/deploy-project)
(def target-dir (u/safer-path project-path "target"))
(def temp-jar-path (u/safer-path target-dir "temp-jar"))
(def jar-out (u/safer-path target-dir "deploy.jar"))
(def install-dir (u/safer-path target-dir "local"))
(def deploy-dir (u/safer-path target-dir "remote"))


(def artefact-name 'deploy)
(def version "1.0")
(def group-id 'group)

(def maven-jar-name (str artefact-name "-" version ".jar"))

(defn make-dest-path [root]
  (apply u/safer-path
         root
         (map str [group-id artefact-name version maven-jar-name])))


(def installed-jar-path (make-dest-path install-dir))
(def deployed-jar-path (make-dest-path deploy-dir))


(def ctxt
  (-> {;; Artefact infos
       :project/working-dir project-path
       :maven/artefact-name artefact-name
       :project/version version
       :maven/group-id (symbol group-id)
       :project/author "Tester"

       ;; Building outputs
       :cleaning/target target-dir
       :maven.pom/dir target-dir
       :jar/temp-output temp-jar-path
       :jar/output jar-out

       ;; Deployment outputs
       :maven.install/dir install-dir
       :maven/server {:maven.server/url (fs/url deploy-dir)}}
      ;; Computed env
      (u/assoc-computed
        :jar/manifest mbt-core/make-manifest
        :project/deps mbt-core/get-deps
        :classpath/index mbt-core/indexed-classpath
        :maven/pom mbt-core/new-pom
        :jar/srcs jar/simple-jar-srcs
        :maven.deploy/artefacts mbt-core/make-usual-artefacts)))


(defn build! [ctxt]
  (-> ctxt
      (u/side-effect! mbt-core/sync-pom!)
      (u/side-effect! mbt-core/add-srcs!)
      (u/side-effect! mbt-core/make-jar-archive!)))


(defn list-jar-files [jar-path]
  (with-open [zfs (mbt-core/open-jar-fs jar-path)]
    (->> zfs
         fs/walk
         fs/realize
         (into #{} (map str)))))


(deftest testing-install-deploy
  (try
    (-> ctxt
        (u/side-effect! build!)
        (u/side-effect! mbt-core/install!)
        (u/side-effect! mbt-core/deploy!))

    (let [jar-content (list-jar-files jar-out)
          installed-jar-content (list-jar-files installed-jar-path)
          deployed-jar-content (list-jar-files deployed-jar-path)]

      (facts
        jar-content => installed-jar-content
        jar-content => deployed-jar-content))
    (catch Exception e
      (throw e))
    (finally
      (mbt-core/clean! ctxt))))
