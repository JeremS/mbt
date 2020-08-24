(ns ^{:author "Jeremy Schoffen"
      :doc "
Api providing default behaviour for maven tasks.
      "}
  fr.jeremyschoffen.mbt.alpha.default.maven
  (:require
    [clojure.spec.alpha :as s]
    [cognitect.anomalies :as anom]
    [fr.jeremyschoffen.mbt.alpha.core :as mbt-core]
    [fr.jeremyschoffen.mbt.alpha.default.jar :as jar]
    [fr.jeremyschoffen.mbt.alpha.default.maven.common :as mc]
    [fr.jeremyschoffen.mbt.alpha.default.specs]
    [fr.jeremyschoffen.mbt.alpha.default.versioning :as v]
    [fr.jeremyschoffen.mbt.alpha.utils :as u]
    [fr.jeremyschoffen.java.nio.alpha.file :as fs]))

(u/def-clone make-usual-artefacts mc/make-usual-artefacts)
(u/def-clone make-usual-artefacts+signatures! mc/make-usual-artefacts+signatures!)

(defn ensure-basic-conf
  "Ensure the necessary basic keys needed to install/deploy maven artefacts are present in the config. Fill the
  blanks with: default values. Those keys are:
    - :jar/output
    - :project/deps
    - :project/version"
  [param]
  (-> param
      (u/ensure-computed
        :jar/output jar/jar-out
        :project/deps mbt-core/deps-get
        :project/version v/current-project-version)))

(u/spec-op ensure-basic-conf
           :deps [jar/jar-out mbt-core/deps-get]
           :param {:req [:build/jar-name
                         :project/output-dir
                         :project/working-dir]})


(defn ensure-install-conf
  "Ensure that the keys needed to install a jar are part of the config.
   The basic ones are taken care of using [[fr.jeremyschoffen.mbt.alpha.default.maven/ensure-basic-conf]].
   The key `:maven.deploy/artefacts` is computed specifically. Default behavior is to make 2 artefacts, one for the jar,
   one for a pom.xml."
  [param]
  (-> param
      ensure-basic-conf
      (u/ensure-computed
        :maven.deploy/artefacts make-usual-artefacts)))

(u/spec-op ensure-install-conf
           :deps [jar/jar-out make-usual-artefacts]
           :param {:req [:build/jar-name
                         :maven.pom/dir
                         :project/output-dir]}
           :ret (s/keys :req [:jar/output :maven.deploy/artefacts]))


(defn check-artefacts-exist
  "Check that the artefacts described under the key `:maven.deploy/artefacts` actually exist."
  [{artefacts :maven.deploy/artefacts
    :as param}]
  (let [missing? (into #{}
                       (comp
                         (map :maven.deploy.artefact/path)
                         (remove fs/exists?))
                       artefacts)]
    (when (seq missing?)
      (throw (ex-info "Missing artefacts when installing/deploying."
                      (merge param
                             {::anom/category ::anom/not-found
                              :missing-artefacts missing?}))))))

(u/spec-op check-artefacts-exist
           :param {:req [:maven.deploy/artefacts]})

(defn install!
  "Install a jar of the current project into the local maven repo.

  Before doing so generate/synchronize a/the pom.xml file to be found in the directory at the `:maven.pom/dir` location.
  If `:maven.deploy/artefacts` isn't provided the default behavior is to generate it using
  [[fr.jeremyschoffen.mbt.alpha.default.maven/ensure-install-conf]]."
  [param]
  (-> param
      ensure-install-conf
      (u/do-side-effect! mbt-core/maven-sync-pom!)
      (u/check check-artefacts-exist)
      mbt-core/maven-install!))

(u/spec-op install!
           :deps [mbt-core/maven-sync-pom! jar/jar-out make-usual-artefacts mbt-core/maven-install!]
           :param {:req [:build/jar-name
                         :maven/artefact-name
                         :maven/group-id
                         :maven.pom/dir
                         :project/output-dir]
                   :opt [:maven/classifier
                         :maven.deploy/artefacts
                         :maven.install/dir]})


(defn ensure-deploy-conf
  "Ensure that the keys needed to deploy a jar are part of the config.
   The basic ones are taken care of using [[fr.jeremyschoffen.mbt.alpha.default.maven/ensure-basic-conf]].

   The key `:maven.deploy/artefacts` is computed here specifically. Default behavior is to make 2 artefacts,
   one for the jar, one for a pom.xml. Also gnupg can be used to sign these artefacts if the parameter under the key
   `:maven.deploy/sign-artefacts?` is true."
  [{sign? :maven.deploy/sign-artefacts?
    :as param}]
  (let [make-deploy-artefacts (if sign?
                                make-usual-artefacts+signatures!
                                make-usual-artefacts)]
    (-> param
        ensure-basic-conf
        (u/ensure-computed
          :maven.deploy/artefacts make-deploy-artefacts))))

(u/spec-op ensure-deploy-conf
           :deps [jar/jar-out make-usual-artefacts make-usual-artefacts+signatures!]
           :param {:req [:build/jar-name
                         :maven.pom/dir
                         :project/output-dir]
                   :opt [:gpg/command
                         :gpg/home-dir
                         :gpg/key-id
                         :gpg/pass-phrase
                         :gpg/version
                         :maven.deploy/sign-artefacts?
                         :project/working-dir]}
           :ret (s/keys :req [:jar/output :maven.deploy/artefacts]))


(defn deploy!
  "Deploy a jar of the current project to a remote repo.

  Before doing so generate/synchronize a/the pom.xml file to be found in the directory at the `:maven.pom/dir` location.
  If `:maven.deploy/artefacts` isn't provided the default behavior is to generate and assoc it to the conf using
  [[fr.jeremyschoffen.mbt.alpha.default.maven/ensure-deploy-conf]]."
  [param]
  (-> param
      ensure-deploy-conf
      (u/do-side-effect! mbt-core/maven-sync-pom!)
      (u/check check-artefacts-exist)
      mbt-core/maven-deploy!))

(u/spec-op deploy!
           :deps [mbt-core/maven-sync-pom!
                  jar/jar-out
                  ensure-deploy-conf
                  mbt-core/maven-deploy!]
           :param {:req [:build/jar-name
                         :maven/artefact-name
                         :maven/group-id
                         :maven/server
                         :maven.pom/dir
                         :project/deps
                         :project/output-dir]
                   :opt [:gpg/command
                         :gpg/home-dir
                         :gpg/key-id
                         :gpg/pass-phrase
                         :gpg/version
                         :maven.deploy/sign-artefacts?
                         :maven/classifier
                         :maven/credentials
                         :maven/local-repo
                         :maven.settings/file
                         :project/working-dir]})
