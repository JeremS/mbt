(ns com.jeremyschoffen.mbt.alpha.default.building.maven-test
  (:require
    [clojure.test :refer [deftest testing]]
    [clojure.spec.test.alpha :as stest]
    [testit.core :refer :all]
    [com.jeremyschoffen.java.nio.alpha.file :as fs]
    [com.jeremyschoffen.mbt.alpha.core :as mbt-core]
    [com.jeremyschoffen.mbt.alpha.default.building :as building]
    [com.jeremyschoffen.mbt.alpha.default.defaults :as defaults]
    [com.jeremyschoffen.mbt.alpha.default.maven :as maven]
    [com.jeremyschoffen.mbt.alpha.test.repos :as test-repos]
    [com.jeremyschoffen.mbt.alpha.utils :as u]))


(stest/instrument [building/ensure-jar-defaults
                   building/jar-out
                   building/jar!

                   maven/install!
                   maven/deploy!

                   mbt-core/clean!])


(def project-path test-repos/deploy-project)
(def target-dir (u/safer-path project-path "target"))
(def install-dir (u/safer-path target-dir "local"))
(def deploy-dir (u/safer-path target-dir "remote"))

(def artefact-name 'deploy)
(def version "1.0")
(def group-id 'group)

(def conf (-> (sorted-map
                :project/working-dir project-path
                :project/version version
                :project/author "Tester"

                :maven/artefact-name artefact-name
                :maven/group-id (symbol group-id)

                :maven.install/dir install-dir
                :maven/server {:maven.server/url (fs/url deploy-dir)})
              defaults/make-context
              building/ensure-jar-defaults))

(def jar-out (building/jar-out conf))

(def maven-jar-name (str artefact-name "-" version ".jar"))

(defn make-dest-path [root]
  (apply u/safer-path
         root
         (map str [group-id artefact-name version maven-jar-name])))


(def installed-jar-path (make-dest-path install-dir))
(def deployed-jar-path (make-dest-path deploy-dir))


(defn list-jar-files [jar-path]
  (with-open [zfs (mbt-core/open-jar-fs jar-path)]
    (->> zfs
         fs/walk
         fs/realize
         (into #{} (map str)))))


(deftest testing-install-deploy
  (try
    (-> conf
        (u/side-effect! building/jar!)
        (u/side-effect! maven/install!)
        (u/side-effect! maven/deploy!))

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
