(ns fr.jeremyschoffen.mbt.alpha.core.maven-test
  (:require
    [clojure.test :refer [deftest testing]]
    [testit.core :refer :all]
    [fr.jeremyschoffen.java.nio.alpha.file :as fs]
    [fr.jeremyschoffen.mbt.alpha.core.maven.deploy :as deploy]
    [fr.jeremyschoffen.mbt.alpha.utils :as u]))

(u/mbt-alpha-pseudo-nss
  maven
  maven.credentials
  maven.server
  maven.settings)

(alter-meta! #'deploy/make-maven-authentication assoc :private false)


(def baseconf
  {::maven/server {::maven.server/id "clojars"}
   ::maven.settings/file (u/safer-path "resources-test" "maven-settings.xml")})


(defn creds [conf]
  (str (deploy/make-maven-authentication conf)))


(deftest credentials
  (facts
    (creds baseconf)
    => "username=clojars-name"

    (-> baseconf
        (assoc ::maven/credentials {::maven.credentials/user-name "tester"
                                    ::maven.credentials/private-key (fs/path "dummy" "path" "to" "key")
                                    ::maven.credentials/password "passwd"
                                    ::maven.credentials/passphrase "phrase"})
        creds)
    => "username=tester, password=***, privateKey.path=dummy/path/to/key, privateKey.passphrase=***"

    (-> baseconf
        (assoc-in [::maven/server ::maven.server/id] "dummy-repo")
        creds)
    => "username=dummy-name, password=***"))
