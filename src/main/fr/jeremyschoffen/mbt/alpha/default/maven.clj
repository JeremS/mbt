(ns ^{:author "Jeremy Schoffen"
      :doc "
Api providing default behaviour for maven tasks.
      "}
  fr.jeremyschoffen.mbt.alpha.default.maven
  (:require
    [clojure.spec.alpha :as s]
    [cognitect.anomalies :as anom]
    [fr.jeremyschoffen.mbt.alpha.core :as mbt-core]
    [fr.jeremyschoffen.mbt.alpha.default.maven.common :as mc]
    [fr.jeremyschoffen.mbt.alpha.default.specs]
    [fr.jeremyschoffen.mbt.alpha.default.versioning :as v]
    [fr.jeremyschoffen.mbt.alpha.utils :as u]
    [fr.jeremyschoffen.java.nio.alpha.file :as fs]))

(u/pseudo-nss
  build
  build.jar
  git
  gpg
  jar
  maven
  maven.deploy
  maven.deploy.artefact
  maven.install
  maven.pom
  maven.settings
  project
  project.deps
  versioning)


(u/def-clone make-usual-artefacts mc/make-usual-artefacts)
(u/def-clone make-usual-artefacts+signatures! mc/make-usual-artefacts+signatures!)

(defn ensure-basic-conf
  "Ensure the necessary basic keys needed to install/deploy maven artefacts are present in the config. Fill the
  blanks with: default values. Those keys are:
    - :fr...mbt.alpha.project/deps
    - :fr...mbt.alpha.project/version"
  [param]
  (-> param
      (u/ensure-computed ::project/version v/current-project-version))) ;;FIXME needs to go too.

(u/spec-op ensure-basic-conf
           :deps [mbt-core/deps-get
                  v/current-project-version]
           :param {:opt [::git/repo
                         ::versioning/scheme
                         ::versioning/tag-base-name]}
           :ret (s/keys :req [::project/version]))


(defn ensure-install-conf
  "Ensure that the keys needed to install a jar are part of the config.
   The basic ones are taken care of using [[fr.jeremyschoffen.mbt.alpha.default.maven/ensure-basic-conf]].
   The key `:fr...mbt.alpha.maven.deploy/artefacts` is computed specifically. Default behavior is to make 2 artefacts,
   one for the jar, one for a pom.xml."
  [param]
  (-> param
      ensure-basic-conf
      (u/ensure-computed
        ::maven.deploy/artefacts make-usual-artefacts)))

(u/spec-op ensure-install-conf
           :deps [ensure-basic-conf
                  make-usual-artefacts]
           :param {:req [::maven.pom/path
                         ::build.jar/path]
                   :opt [::git/repo
                         ::versioning/scheme
                         ::versioning/tag-base-name]}
           :ret (s/keys :req [::maven.deploy/artefacts]))


(defn check-artefacts-exist
  "Check that the artefacts described under the key `:fr...mbt.alpha.maven.deploy/artefacts` actually exist."
  [{artefacts ::maven.deploy/artefacts
    :as param}]
  (let [missing? (into #{}
                       (comp
                         (map ::maven.deploy.artefact/path)
                         (remove fs/exists?))
                       artefacts)]
    (when (seq missing?)
      (throw (ex-info "Missing artefacts when installing/deploying."
                      (merge param
                             {::anom/category ::anom/not-found
                              :missing-artefacts missing?}))))))

(u/spec-op check-artefacts-exist
           :param {:req [::maven.deploy/artefacts]})

(defn install!
  "Install a jar of the current project into the local maven repo.

  Before doing so generate/synchronize a/the pom.xml file to be found in the directory at the
  `:fr...mbt.alpha.maven.pom/dir` location. If `:fr...mbt.alpha.maven.deploy/artefacts` isn't provided the default
  behavior is to generate it using [[fr.jeremyschoffen.mbt.alpha.default.maven/ensure-install-conf]]."
  [param]
  (-> param
      ensure-install-conf
      (u/do-side-effect! mbt-core/maven-sync-pom!)
      (u/check check-artefacts-exist)
      mbt-core/maven-install!))

(u/spec-op install!
           :deps [ensure-install-conf
                  mbt-core/maven-sync-pom!
                  mbt-core/maven-install!]
           :param {:req #{::build.jar/path
                          ::maven/artefact-name
                          ::maven/group-id
                          ::maven.pom/path
                          ::project/deps}
                   :opt #{::git/repo
                          ::maven/classifier
                          ::maven.deploy/artefacts
                          ::maven/scm
                          ::maven.install/dir
                          ::project/licenses
                          ::project/version
                          ::versioning/scheme
                          ::versioning/tag-base-name}})


(defn ensure-deploy-conf
  "Ensure that the keys needed to deploy a jar are part of the config.
   The basic ones are taken care of using [[fr.jeremyschoffen.mbt.alpha.default.maven/ensure-basic-conf]].

   The key `:fr...mbt.alpha.maven.deploy/artefacts` is computed here specifically. Default behavior is to make 2
   artefacts, one for the jar, one for a pom.xml. Also gnupg can be used to sign these artefacts if the parameter under
   the key `:fr...mbt.alpha.maven.deploy/sign-artefacts?` is true."
  [{sign? ::maven.deploy/sign-artefacts?
    :as param}]
  (let [make-deploy-artefacts (if sign?
                                make-usual-artefacts+signatures!
                                make-usual-artefacts)]
    (-> param
        ensure-basic-conf
        (u/ensure-computed
          ::maven.deploy/artefacts make-deploy-artefacts))))

(u/spec-op ensure-deploy-conf
           :deps [ensure-basic-conf
                  make-usual-artefacts
                  make-usual-artefacts+signatures!]
           :param {:req [::maven.pom/path
                         ::build.jar/path]
                   :opt #{::git/repo
                          ::gpg/command
                          ::gpg/home-dir
                          ::gpg/key-id
                          ::gpg/pass-phrase
                          ::gpg/version
                          ::project/working-dir
                          ::versioning/scheme
                          ::versioning/tag-base-name}}
           :ret (s/keys :req [::maven.deploy/artefacts]))


(defn deploy!
  "Deploy a jar of the current project to a remote repo.

  Before doing so generate/synchronize a/the pom.xml file to be found in the directory at the
  `:fr...mbt.alpha.maven.pom/dir` location. If `:fr...mbt.alpha.maven.deploy/artefacts` isn't provided the default
  behavior is to generate and assoc it to the conf using
  [[fr.jeremyschoffen.mbt.alpha.default.maven/ensure-deploy-conf]]."
  [param]
  (-> param
      ensure-deploy-conf
      (u/do-side-effect! mbt-core/maven-sync-pom!)
      (u/check check-artefacts-exist)
      mbt-core/maven-deploy!))

(u/spec-op deploy!
           :deps [mbt-core/maven-sync-pom!
                  ensure-deploy-conf
                  mbt-core/maven-deploy!]
           :param {:req [::build.jar/path
                         ::maven/artefact-name
                         ::maven/group-id
                         ::maven/server
                         ::maven.pom/path
                         ::project/deps]
                   :opt [::gpg/command
                         ::gpg/home-dir
                         ::gpg/key-id
                         ::gpg/pass-phrase
                         ::gpg/version
                         ::maven.deploy/artefacts
                         ::maven.deploy/sign-artefacts?
                         ::maven/classifier
                         ::maven/credentials
                         ::maven/local-repo
                         ::maven.settings/file
                         ::project/working-dir]})
