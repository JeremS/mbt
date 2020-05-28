(ns com.jeremyschoffen.mbt.alpha.building.deploy
  (:require
    [clojure.tools.deps.alpha.extensions.maven]
    [clojure.tools.deps.alpha.util.maven :as maven]
    [com.jeremyschoffen.java.nio.file :as fs]
    [com.jeremyschoffen.mbt.alpha.building.gpg :as gpg]
    [com.jeremyschoffen.mbt.alpha.specs]
    [com.jeremyschoffen.mbt.alpha.utils :as u])
  (:import
    [org.apache.maven.properties.internal SystemProperties]
    [org.apache.maven.settings Server Settings]
    [org.apache.maven.settings.building DefaultSettingsBuildingRequest DefaultSettingsBuilderFactory]
    [org.eclipse.aether.artifact DefaultArtifact]
    [org.eclipse.aether.deployment DeployRequest]
    [org.eclipse.aether.repository RemoteRepository$Builder]
    [org.eclipse.aether.util.repository AuthenticationBuilder]))

;; inspired by https://github.com/EwenG/badigeon/blob/master/src/badigeon/deploy.clj





(defn make-usual-artefacts
  "Makes a sequence of maps representing maven artefacts (in the deployment sense where an artefact
  is one of the different files that will be pushed to a maven server).

  Here representations for a pom.xml and a jar are made."
  [{pom-dir :maven.pom/dir
    jar-path :jar/output}]
  [{:maven.deploy.artefact/path (fs/path pom-dir "pom.xml")
    :maven.deploy.artefact/extension "pom"}

   {:maven.deploy.artefact/path jar-path
    :maven.deploy.artefact/extension "jar"}])

(u/spec-op make-usual-artefacts
           :param {:req [:maven.pom/dir
                         :jar/output]})


(defn sign-artefact!
  "Signs one maven deployment artefact using gpg."
  [{artefact :maven.deploy/artefact
    sign-key  :gpg/key-id
    :as param}]
  (let [{p :maven.deploy.artefact/path
         ext :maven.deploy.artefact/extension} artefact
        signature-path (gpg/make-sign-out p)]
    (gpg/sign-file!
      (assoc param
        :gpg.sign/spec (cond-> {:gpg.sign/in p
                                :gpg.sign/out signature-path}

                               sign-key (assoc :gpg/key-id sign-key))))
    {:maven.deploy.artefact/path signature-path
     :maven.deploy.artefact/extension (str ext ".asc")}))

(u/spec-op sign-artefact!
           :deps [gpg/sign-file!]
           :param {:req [:maven.deploy/artefact]
                   :opt [:project/working-dir
                         :gpg/key-id]}
           :ret :maven.deploy/artefact)


(defn sign-artefacts!
  "Signs several maven deployment artefact using gpg."
  [{artefacts :maven.deploy/artefacts
    :as param}]
  (mapv (fn [a]
          (sign-artefact! (assoc param :maven.deploy/artefact a)))
        artefacts))

(u/spec-op sign-artefacts!
           :deps [sign-artefact!]
           :param {:req [:maven.deploy/artefacts]
                   :opt [:project/working-dir
                         :gpg/key-id]}
           :ret :maven.deploy/artefacts)


(defn make-usual-artefacts+signatures!
  "Makes a sequence of maps representing maven artefacts (in the deployment sense where an artefact
  is one of the different files that will be pushed to a maven server) and gpg sigs them.

  Here representations for a pom.xml and a jar are made. The project jar and pom are signed with
  gpg. The artefact represntations for the signatures are returned."
  [ctxt]
  (let [artefacts (make-usual-artefacts ctxt)
        signatures (sign-artefacts!
                     (assoc ctxt :maven.deploy/artefacts artefacts))]
    (into artefacts signatures)))

(u/spec-op make-usual-artefacts+signatures!
           :deps [make-usual-artefacts sign-artefacts!]
           :param {:req [:jar/output :maven.pom/dir]
                   :opt [:gpg/key-id :project/working-dir]}
           :ret :maven.deploy/artefacts)


(defn- mbt-artefact->maven-artefact
  [{artefact-name :artefact/name
    group-id :maven/group-id
    classifier :maven/classifier
    version :project/version
    deploy-artefact :maven.deploy/artefact}]
  (let [{:maven.deploy.artefact/keys [path extension]} deploy-artefact]
    (.setFile (DefaultArtifact. (str group-id)
                                (str artefact-name)
                                (str classifier)
                                extension
                                (str version))
              (fs/file path))))

(u/spec-op mbt-artefact->maven-artefact
           :param {:req [:artefact/name
                         :maven/group-id
                         :project/version
                         :maven.deploy/artefact]
                   :opt [:maven/classifier]})


(defn- mbt-artefacts->maven-artefacts
  [{deploy-artefacts :maven.deploy/artefacts
    :as param}]
  (mapv (fn [a]
          (-> param
              (assoc :maven.deploy/artefact a)
              mbt-artefact->maven-artefact))
        deploy-artefacts))

(u/spec-op mbt-artefacts->maven-artefacts
           :param {:req [:artefact/name
                         :maven/group-id
                         :project/version
                         :maven.deploy/artefacts]
                   :opt [:maven/classifier]})



;; remake of org.apache.maven.settings.DefaultMavenSettingsBuilder method buildSettings
;; TODO: Find a way to get the global maven settings.
(defn ^Settings get-maven-settings [{maven-settings-file :maven.settings/file
                                     :or {maven-settings-file u/maven-default-settings-file}}]
  (let [settings-builder (.newInstance (DefaultSettingsBuilderFactory.))
        settings-building-request (-> (DefaultSettingsBuildingRequest.)
                                      (.setUserSettingsFile (fs/file maven-settings-file))
                                      (.setSystemProperties (SystemProperties/getSystemProperties)))
        settings-building-result (.build settings-builder settings-building-request)]
    (.getEffectiveSettings settings-building-result)))

(u/spec-op get-maven-settings
           :param {:opt [:maven.settings/file]})


(defn- ^Server settings-for-server [{server :maven/server
                                     :as param}]
  (when-let [server-id (:maven.server/id server)]
    (->> (get-maven-settings param)
         .getServers
         (filter (fn [^Server server]
                   (.equalsIgnoreCase ^String server-id (.getId server))))
         first)))

(u/spec-op settings-for-server
           :deps [get-maven-settings]
           :param {:req [:maven/server]
                   :opt [:maven.settings/file]})


(defn- make-maven-authentication [{credentials    :maven/credentials
                                   :as param}]
  (let [server-settings (settings-for-server param)
        {user-name :maven.credentials/user-name
         password :maven.credentials/password
         private-key :maven.credentials/private-key
         passphrase :maven.credentials/passphrase
         :or {user-name (some-> server-settings .getUsername)
              password (some-> server-settings .getPassword)
              private-key (some-> server-settings .getPrivateKey)
              passphrase (some-> server-settings .getPassphrase)}} credentials
        ^AuthenticationBuilder builder (AuthenticationBuilder.)]
    (-> builder
      (.addUsername user-name)
      (.addPassword ^String password)
      (.addPrivateKey ^String private-key ^String passphrase)
      (.build))))

(u/spec-op make-maven-authentication
           :deps [settings-for-server]
           :param {:req [:maven/server]
                   :opt [:maven/credentials
                         :maven.settings/file]})



(defn- make-remote-repo [{server :maven/server
                          cred   :maven/credentials
                          :as param}]
  (let [{:maven.server/keys [id url]} server]
    (-> (RemoteRepository$Builder. id "default" (str url))
        (cond-> cred
                (.setAuthentication (make-maven-authentication param)))
        .build)))

(u/spec-op make-remote-repo
           :deps [make-maven-authentication]
           :param {:req [:maven/server]
                   :opt [:maven/credentials]})


(defn- make-deploy-request [param]
  (let [repo (make-remote-repo param)
        artefacts (mbt-artefacts->maven-artefacts param)
        request (DeployRequest.)]
    (do
      (.setRepository request repo)
      (reduce (fn [^DeployRequest req artefact]
                (.addArtifact req artefact))
              request artefacts))
    request))

        

(u/spec-op make-deploy-request
           :deps [make-remote-repo mbt-artefacts->maven-artefacts]
           :param {:req [:artefact/name
                         :maven/group-id
                         :project/version
                         :maven/server
                         :maven.deploy/artefacts]
                   :opt [:maven/classifier
                         :maven/credentials
                         :maven.settings/file]})




(defn deploy! [param]
  (java.lang.System/setProperty "aether.checksums.forSignature" "true")
  (let [system (maven/make-system)
        session (maven/make-session system maven/default-local-repo)
        deploy-request (make-deploy-request param)]
    (.deploy system session deploy-request)))

(u/spec-op deploy!
           :deps [make-deploy-request]
           :param {:req [:artefact/name
                         :maven/group-id
                         :project/version
                         :maven/server
                         :maven.deploy/artefacts]
                   :opt [:maven/classifier
                         :maven/credentials
                         :maven.settings/file]})

(u/param-suggestions deploy!)


(require '[clojure.spec.test.alpha :as spec-test])
(spec-test/instrument)

(u/param-suggestions make-maven-authentication)
(comment
  ;; generate a jar and a pom.
  (do
    (require '[com.jeremyschoffen.mbt.alpha.building.deps :as deps])
    (require '[com.jeremyschoffen.mbt.alpha.building.classpath :as cp])
    (require '[com.jeremyschoffen.mbt.alpha.building.pom :as pom])
    (require '[com.jeremyschoffen.mbt.alpha.building.jar :as jar])


    (u/param-specs pom/sync-pom!)

    (def wd-path (u/safer-path "test-repos" "deploy"))
    (def target-path (u/safer-path wd-path "target"))
    (def temp-jar-path (u/safer-path target-path "tempjar"))
    (def jar-path (u/safer-path target-path "deploy.jar"))
    (def remote-url (-> target-path
                        (fs/path "remote")
                        fs/url))

    (def ctxt (-> {:project/working-dir wd-path
                   :artefact/name "deploy"
                   :maven/group-id 'group.deploy
                   :project/version "1.1"
                   :maven.pom/dir target-path
                   :jar/temp-output temp-jar-path
                   :jar/output jar-path
                   :maven/server {:maven.server/url remote-url}}

                  (u/assoc-computed
                    :project/deps deps/get-deps
                    :classpath/index cp/indexed-classpath))))


  (defn pom! [ctxt]
    (pom/sync-pom! ctxt))

  (defn jar! [ctxt]
    (-> ctxt
        (u/assoc-computed
          :maven/pom pom/new-pom
          :jar/srcs jar/simple-jar-srcs)
        (u/side-effect! jar/add-srcs!)
        (u/side-effect! jar/jar!)))



  (str (make-maven-authentication {:maven/server {:maven.server/id "clojars"}
                                   :maven/credentials {:maven.credentials/password "admin"}}))

  (bean (make-remote-repo ctxt))

  (def res (-> ctxt
               (u/assoc-computed :maven.deploy/artefacts make-usual-artefacts+signatures!)
               deploy!))

  (bean res)

  (->> (u/safer-path "test-repos" "deploy")
       fs/walk
       fs/realize
       (filter (fn [p] (fs/ancestor? target-path p)))))



