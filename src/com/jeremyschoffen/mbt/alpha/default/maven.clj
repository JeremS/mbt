(ns com.jeremyschoffen.mbt.alpha.default.maven
  (:require
    [clojure.spec.alpha :as s]
    [cognitect.anomalies :as anom]
    [com.jeremyschoffen.mbt.alpha.core :as mbt-core]
    [com.jeremyschoffen.mbt.alpha.default.building :as b]
    [com.jeremyschoffen.mbt.alpha.default.maven.common :as mc]
    [com.jeremyschoffen.mbt.alpha.default.specs]
    [com.jeremyschoffen.mbt.alpha.default.versioning :as v]
    [com.jeremyschoffen.mbt.alpha.utils :as u]
    [com.jeremyschoffen.java.nio.alpha.file :as fs]))

(u/alias-fn make-usual-artefacts mc/make-usual-artefacts)
(u/alias-fn make-usual-artefacts+signatures! mc/make-usual-artefacts+signatures!)

(defn ensure-basic-conf [param]
  (-> param
      (u/ensure-computed
        :jar/output b/jar-out
        :project/deps mbt-core/get-deps
        :project/version v/current-project-version)))

(u/spec-op ensure-basic-conf
           :deps [b/jar-out mbt-core/get-deps]
           :param {:req [:build/jar-name
                         :project/output-dir
                         :project/working-dir]})


(defn ensure-install-conf [param]
  (-> param
      ensure-basic-conf
      (u/ensure-computed
        :maven.deploy/artefacts make-usual-artefacts)))

(u/spec-op ensure-install-conf
           :deps [b/jar-out make-usual-artefacts]
           :param {:req [:build/jar-name
                         :maven.pom/dir
                         :project/output-dir]}
           :ret (s/keys :req [:jar/output :maven.deploy/artefacts]))


(defn check-artefacts-exist [{artefacts :maven.deploy/artefacts
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

(defn install! [param]
  (-> param
      ensure-install-conf
      (u/side-effect! mbt-core/sync-pom!)
      (u/check check-artefacts-exist)
      mbt-core/install!))

(u/spec-op install!
           :deps [mbt-core/sync-pom! b/jar-out make-usual-artefacts mbt-core/install!]
           :param {:req [:build/jar-name
                         :maven/artefact-name
                         :maven/group-id
                         :maven.pom/dir
                         :project/output-dir]
                   :opt [:maven/classifier :maven.install/dir]})


(defn ensure-deploy-conf [{sign? :maven.deploy/sign-artefacts?
                           :as param}]
  (let [make-deploy-artefacts (if sign?
                                make-usual-artefacts+signatures!
                                make-usual-artefacts)]
    (-> param
        ensure-basic-conf
        (u/ensure-computed
          :maven.deploy/artefacts make-deploy-artefacts))))

(u/spec-op ensure-deploy-conf
           :deps [b/jar-out make-usual-artefacts make-usual-artefacts+signatures!]
           :param {:req [:build/jar-name
                         :maven.pom/dir
                         :project/output-dir]
                   :opt [:gpg/key-id
                         :maven.deploy/sign-artefacts?
                         :project/working-dir]
                   :ret (s/keys :req [:jar/output :maven.deploy/artefacts])})


(defn deploy! [param]
  (-> param
      ensure-deploy-conf
      (u/side-effect! mbt-core/sync-pom!)
      (u/check check-artefacts-exist)
      mbt-core/deploy!))

(u/spec-op deploy!
           :deps [mbt-core/sync-pom!
                  b/jar-out
                  make-usual-artefacts
                  make-usual-artefacts+signatures!
                  mbt-core/deploy!]
           :param {:req [:build/jar-name
                         :maven/artefact-name
                         :maven/group-id
                         :maven/server
                         :maven.pom/dir
                         :project/deps
                         :project/output-dir]
                   :opt [:gpg/key-id
                         :maven.deploy/sign-artefacts?
                         :maven/classifier
                         :maven/credentials
                         :maven/local-repo
                         :maven.settings/file
                         :project/working-dir]})
