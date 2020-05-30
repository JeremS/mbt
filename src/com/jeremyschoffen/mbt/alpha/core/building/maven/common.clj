(ns com.jeremyschoffen.mbt.alpha.core.building.maven.common
  (:require
    [com.jeremyschoffen.java.nio.file :as fs]
    [com.jeremyschoffen.mbt.alpha.core.building.gpg :as gpg]
    [com.jeremyschoffen.mbt.alpha.core.utils :as u])
  (:import
    [org.eclipse.aether.artifact DefaultArtifact]))


(defn make-maven-artefact
  "Constructor org.eclipse.aether.artifact.DefaultArtifact."
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

(u/spec-op make-maven-artefact
           :param {:req [:artefact/name
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
           :param {:req [:artefact/name
                         :maven/group-id
                         :project/version
                         :maven.deploy/artefacts]
                   :opt [:maven/classifier]})


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
                         :jar/output]}
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