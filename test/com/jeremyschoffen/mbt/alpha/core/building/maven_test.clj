(ns com.jeremyschoffen.mbt.alpha.core.building.maven-test
  (:require
    [clojure.test :refer [deftest testing]]
    [clojure.spec.test.alpha :as stest]
    [testit.core :refer :all]
    [com.jeremyschoffen.java.nio.file :as fs]
    [com.jeremyschoffen.mbt.alpha.core.building.classpath :as cp]
    [com.jeremyschoffen.mbt.alpha.core.building.cleaning :as cleaning]
    [com.jeremyschoffen.mbt.alpha.core.building.deps :as deps]
    [com.jeremyschoffen.mbt.alpha.core.building.jar :as jar]
    [com.jeremyschoffen.mbt.alpha.core.building.maven.common :as common]
    [com.jeremyschoffen.mbt.alpha.core.building.maven.deploy :as deploy]
    [com.jeremyschoffen.mbt.alpha.core.building.maven.install :as install]
    [com.jeremyschoffen.mbt.alpha.core.building.maven.pom :as pom]
    [com.jeremyschoffen.mbt.alpha.core.utils :as u]))


(stest/instrument)


(def project-path (u/safer-path "test-repos" "deploy"))
(def target-dir (u/safer-path project-path "target"))
(def temp-jar-path (u/safer-path target-dir "temp-jar"))
(def jar-out (u/safer-path target-dir "deploy.jar"))
(def install-dir (u/safer-path target-dir "local"))
(def deploy-dir (u/safer-path target-dir "remote"))


(def artefact-name "deploy")
(def version "1.0")
(def group-id "group")

(def maven-jar-name (str artefact-name "-" version ".jar"))

(defn make-dest-path [root]
  (u/safer-path root group-id artefact-name version maven-jar-name))


(def installed-jar-path (make-dest-path install-dir))
(def deployed-jar-path (make-dest-path deploy-dir))


(def ctxt
  (-> {;; Artefact infos
       :project/working-dir project-path
       :artefact/name artefact-name
       :project/version version
       :maven/group-id (symbol group-id)
       :project/author "Tester"

       ;; Building outputs
       :cleaning/target target-dir
       :maven.pom/dir target-dir
       :jar/temp-output temp-jar-path
       :jar/output jar-out

       ;; Deployment outputs
       :maven/local-repo install-dir
       :maven/server {:maven.server/url (fs/url deploy-dir)}}
      ;; Computed env
      (u/assoc-computed
        :project/deps deps/get-deps
        :classpath/index cp/indexed-classpath
        :maven/pom pom/new-pom
        :jar/srcs jar/simple-jar-srcs
        :maven.deploy/artefacts common/make-usual-artefacts)))


(defn build! [ctxt]
  (-> ctxt
      (u/side-effect! pom/sync-pom!)
      (u/side-effect! jar/add-srcs!)
      (u/side-effect! jar/make-jar-archive!)))


(defn list-jar-files [jar-path]
  (with-open [zfs (jar/open-jar-fs jar-path)]
    (->> zfs
         fs/walk
         fs/realize
         (into #{} (map str)))))


(deftest testing-install-deploy
  (-> ctxt
      (u/side-effect! build!)
      (u/side-effect! install/install!)
      (u/side-effect! deploy/deploy!))

  (let [jar-content (list-jar-files jar-out)
        installed-jar-content (list-jar-files installed-jar-path)
        deployed-jar-content (list-jar-files deployed-jar-path)]

    (facts
      jar-content => installed-jar-content
      jar-content => deployed-jar-content))

  (cleaning/clean! ctxt))
