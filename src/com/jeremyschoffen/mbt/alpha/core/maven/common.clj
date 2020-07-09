(ns com.jeremyschoffen.mbt.alpha.core.maven.common
  (:require
    [com.jeremyschoffen.java.nio.alpha.file :as fs]
    [com.jeremyschoffen.mbt.alpha.core.building.gpg :as gpg]
    [com.jeremyschoffen.mbt.alpha.utils :as u])
  (:import
    [org.eclipse.aether.artifact DefaultArtifact]))

;;----------------------------------------------------------------------------------------------------------------------
;; Maven Constants
;;----------------------------------------------------------------------------------------------------------------------
(def home (u/safer-path (System/getProperty "user.home")))
(def default-local-repo (u/safer-path home ".m2"))
(def maven-default-settings-file (u/safer-path default-local-repo "settings.xml"))


;;----------------------------------------------------------------------------------------------------------------------
;; Deploy utils
;;----------------------------------------------------------------------------------------------------------------------
(defn make-maven-artefact
  "Constructor org.eclipse.aether.artifact.DefaultArtifact."
  [{artefact-name :maven/artefact-name
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

(u/spec-op make-maven-artefact
           :param {:req [:maven/artefact-name
                         :maven/group-id
                         :project/version
                         :maven.deploy/artefact]
                   :opt [:maven/classifier]})


(defn make-maven-artefacts
  "Constructs several org.eclipse.aether.artifact.DefaultArtifact."
  [{deploy-artefacts :maven.deploy/artefacts
    :as param}]
  (mapv (fn [a]
          (-> param
              (assoc :maven.deploy/artefact a)
              make-maven-artefact))
        deploy-artefacts))

(u/spec-op make-maven-artefacts
           :param {:req [:maven/artefact-name
                         :maven/group-id
                         :project/version
                         :maven.deploy/artefacts]
                   :opt [:maven/classifier]})


(defn sign-artefact!
  "Signs one maven deployment artefact using gpg, return a map
  specifiying the resulting signature as instance :maven.deploy/artefact spec."
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
