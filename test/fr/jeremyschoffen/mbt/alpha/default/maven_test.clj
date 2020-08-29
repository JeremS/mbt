(ns fr.jeremyschoffen.mbt.alpha.default.maven-test
  (:require
    [clojure.test :refer [deftest testing]]
    [clojure.spec.test.alpha :as st]
    [testit.core :refer :all]
    [fr.jeremyschoffen.java.nio.alpha.file :as fs]
    [fr.jeremyschoffen.mbt.alpha.core :as mbt-core]
    [fr.jeremyschoffen.mbt.alpha.default.config :as config]
    [fr.jeremyschoffen.mbt.alpha.default.jar :as jar]
    [fr.jeremyschoffen.mbt.alpha.default.maven :as default-maven]
    [fr.jeremyschoffen.mbt.alpha.test.repos :as test-repos]
    [fr.jeremyschoffen.mbt.alpha.utils :as u]))


(u/pseudo-nss
  build.jar
  maven
  maven.install
  maven.server
  project)


(st/instrument `[jar/ensure-jar-defaults
                 jar/jar!

                 default-maven/install!
                 default-maven/deploy!

                 mbt-core/clean!])


(def project-path test-repos/deploy-project)
(def target-dir (u/safer-path project-path "target"))
(def install-dir (u/safer-path target-dir "local"))
(def deploy-dir (u/safer-path target-dir "remote"))

(def artefact-name 'deploy)
(def version "1.0")
(def group-id 'group)

(def conf (-> (sorted-map
                ::project/working-dir project-path
                ::project/version version
                ::project/author "Tester"

                ::maven/artefact-name artefact-name
                ::maven/group-id (symbol group-id)

                ::maven.install/dir install-dir
                ::maven/server {::maven.server/url (fs/url deploy-dir)})
              config/make-base-config
              jar/ensure-jar-defaults))

(def jar-out (::build.jar/path conf))

(def maven-jar-name (str artefact-name "-" version ".jar"))

(defn make-dest-path [root]
  (apply u/safer-path
         root
         (map str [group-id artefact-name version maven-jar-name])))


(def installed-jar-path (make-dest-path install-dir))
(def deployed-jar-path (make-dest-path deploy-dir))


(defn list-jar-files [jar-path]
  (with-open [zfs (mbt-core/jar-read-only-jar-fs jar-path)]
    (->> zfs
         fs/walk
         fs/realize
         (into #{} (map str)))))


(deftest testing-install-deploy
  (try
    (-> conf
        (u/side-effect! jar/jar!)
        (u/side-effect! default-maven/install!)
        (u/side-effect! default-maven/deploy!))

    (let [jar-content (list-jar-files jar-out)
          installed-jar-content (list-jar-files installed-jar-path)
          deployed-jar-content (list-jar-files deployed-jar-path)]

      (facts
        jar-content => installed-jar-content
        jar-content => deployed-jar-content))
    (catch Exception e
      (throw e))
    (finally
      (mbt-core/clean! conf))))