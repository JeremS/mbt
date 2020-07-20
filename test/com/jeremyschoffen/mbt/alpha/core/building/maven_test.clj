(ns com.jeremyschoffen.mbt.alpha.core.building.maven-test
  (:require
    [clojure.test :refer [deftest testing]]
    [testit.core :refer :all]
    [com.jeremyschoffen.java.nio.alpha.file :as fs]
    [com.jeremyschoffen.mbt.alpha.core.maven.deploy :as deploy]
    [com.jeremyschoffen.mbt.alpha.utils :as u]))


(alter-meta! #'deploy/make-maven-authentication assoc :private false)


(def baseconf
  {:maven/server {:maven.server/id "clojars"}
   :maven.settings/file (u/safer-path "resources-test" "maven-settings.xml")})


(defn creds [conf]
  (str (deploy/make-maven-authentication conf)))


(deftest credentials
  (facts
    (creds baseconf)
    => "username=clojars-name"

    (-> baseconf
        (assoc :maven/credentials {:maven.credentials/user-name "tester"
                                   :maven.credentials/private-key "tester"
                                   :maven.credentials/password "passwd"
                                   :maven.credentials/passphrase "phrase"})
        creds)
    => "username=tester, password=***, privateKey.path=tester, privateKey.passphrase=***"

    (-> baseconf
        (assoc-in [:maven/server :maven.server/id] "dummy-repo")
        creds)
    => "username=dummy-name, password=***"))
