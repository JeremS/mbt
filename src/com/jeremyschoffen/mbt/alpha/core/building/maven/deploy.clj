(ns com.jeremyschoffen.mbt.alpha.core.building.maven.deploy
  (:require
    [clojure.tools.deps.alpha.extensions.maven]
    [clojure.tools.deps.alpha.util.maven :as maven]
    [com.jeremyschoffen.java.nio.file :as fs]
    [com.jeremyschoffen.mbt.alpha.core.building.maven.common :as common]
    [com.jeremyschoffen.mbt.alpha.core.specs]
    [com.jeremyschoffen.mbt.alpha.core.utils :as u])
  (:import
    [org.apache.maven.properties.internal SystemProperties]
    [org.apache.maven.settings Server Settings]
    [org.apache.maven.settings.building DefaultSettingsBuildingRequest DefaultSettingsBuilderFactory]
    [org.eclipse.aether.deployment DeployRequest]
    [org.eclipse.aether.repository RemoteRepository$Builder]
    [org.eclipse.aether.util.repository AuthenticationBuilder]))


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
        artefacts (common/make-maven-artefacts param)
        request (DeployRequest.)]
    (do
      (.setRepository request repo)
      (reduce (fn [^DeployRequest req artefact]
                (.addArtifact req artefact))
              request artefacts))
    request))

(u/spec-op make-deploy-request
           :deps [make-remote-repo common/make-maven-artefacts]
           :param {:req [:artefact/name
                         :maven/group-id
                         :project/version
                         :maven/server
                         :maven.deploy/artefacts]
                   :opt [:maven/classifier
                         :maven/credentials
                         :maven.settings/file]})


(defn deploy! [param]
  (System/setProperty "aether.checksums.forSignature" "true")
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
