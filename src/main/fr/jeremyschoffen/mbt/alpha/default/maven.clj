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

  This function takes care of several operations. It starts by ensuring that the config has a
  `:fr...mbt.alpha.maven.deploy/artefacts` key, compute a value with
  [[fr.jeremyschoffen.mbt.alpha.default.maven/make-usual-artefacts]] if necessary. Next it
  generates/synchronizes a/the pom.xml file at the `:fr...mbt.alpha.maven.pom/path` location.

  After checking the actual existence of the deployment artefacts uses maven to install them.
  "
  [param]
  (-> param
      (u/ensure-computed ::maven.deploy/artefacts make-usual-artefacts)
      (u/do-side-effect! mbt-core/maven-sync-pom!)
      (u/check check-artefacts-exist)
      mbt-core/maven-install!))

(u/spec-op install!
           :deps [make-usual-artefacts
                  mbt-core/maven-sync-pom!
                  mbt-core/maven-install!]
           :param {:req #{::build.jar/path
                          ::maven/artefact-name
                          ::maven/group-id
                          ::maven.pom/path
                          ::project/deps
                          ::project/version}
                   :opt #{::maven.deploy/artefacts
                          ::maven/classifier
                          ::maven/scm
                          ::maven.install/dir
                          ::project/licenses}})


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
        (u/ensure-computed ::maven.deploy/artefacts make-deploy-artefacts))))

(u/spec-op ensure-deploy-conf
           :deps [make-usual-artefacts
                  make-usual-artefacts+signatures!]
           :param {:req [::maven.pom/path
                         ::build.jar/path]
                   :opt #{::gpg/command
                          ::gpg/home-dir
                          ::gpg/key-id
                          ::gpg/pass-phrase
                          ::gpg/version
                          ::project/working-dir}}
           :ret (s/keys :req [::maven.deploy/artefacts]))


(defn deploy!
  "Deploy a jar of the current project into the a remote maven repo.
  This function takes care of several operations. It starts by ensuring that the config has a
  `:fr...mbt.alpha.maven.deploy/artefacts` key, compute a value with
  [[fr.jeremyschoffen.mbt.alpha.default.maven/make-usual-artefacts]] if necessary. Next it
  generates/synchronizes a/the pom.xml file at the `:fr...mbt.alpha.maven.pom/path` location.

  After checking the actual existence of the deployment artefacts uses maven to install them.

  "
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
                         ::project/deps
                         ::project/version]
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
                         ::maven/scm
                         ::maven.settings/file
                         ::project/licenses
                         ::project/working-dir]})
