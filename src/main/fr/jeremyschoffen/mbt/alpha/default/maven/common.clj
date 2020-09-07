(ns ^{:author "Jeremy Schoffen"
      :doc "
Common maven utilities used in the default apis.
      "}
  fr.jeremyschoffen.mbt.alpha.default.maven.common
  (:require
    [lambdaisland.regal :as regal]

    [fr.jeremyschoffen.mbt.alpha.core :as mbt-core]
    [fr.jeremyschoffen.mbt.alpha.default.specs]
    [fr.jeremyschoffen.mbt.alpha.utils :as u]))



(u/pseudo-nss
  build.jar
  gpg
  jar
  maven
  maven.deploy
  maven.deploy.artefact
  maven.pom
  maven.scm
  project)


;;----------------------------------------------------------------------------------------------------------------------
;; Maven Scm stuff
;;----------------------------------------------------------------------------------------------------------------------
;; inspired by https://github.com/technomancy/leiningen/blob/master/src/leiningen/pom.clj
(def alphas [:class [\A \Z] [\a \z]])

(def domain-prefix-r  [:cat [:repeat alphas 2 100] \@])

(def http-protocol-r [:+ [:not ":"]])

(def pre-domain-regal [:cat http-protocol-r "://" [:? domain-prefix-r]])

(def capture-r [:capture [:+ [:not "/"]]])
(def domain-r capture-r)
(def user-r capture-r)
(def project-r capture-r)

(def suffix-r [:? ".git"])

(def github-like-r [:cat
                    pre-domain-regal
                    domain-r "/"
                    user-r "/"
                    project-r
                    suffix-r])


(def github-like-regex (regal/regex github-like-r))


(defn make-github-like-scm-map
  "Take a url like `https://github.com/user/project` under the key `;...mbt.alpha.project/git-url`
  and turns it into:
  ```
  #:...mbt.alpha.maven.scm{:connection \"scm:git:git://github.com/user/project.git\"
                           :developer-connection \"scm:git:ssh://git@github.com/user/project.git\",
                           :url \"https://github.com/user/project\"}
  ```"
  [{url ::project/git-url}]
  (if url
    (let [[_ domain user repo] (re-matches github-like-regex url)]
      (if (and domain user repo)
        {::maven.scm/connection (format "scm:git:git://%s/%s/%s.git" domain user repo)
         ::maven.scm/developer-connection (format "scm:git:ssh://git@%s/%s/%s.git" domain user repo)
         ::maven.scm/url (format "https://%s/%s/%s" domain user repo)}
        {}))
    {}))


(u/spec-op make-github-like-scm-map
           :param {:opt [::project/git-url]}
           :ret ::maven/scm)


;;----------------------------------------------------------------------------------------------------------------------
;; Maven deploy artefacts
;;----------------------------------------------------------------------------------------------------------------------
(defn make-usual-artefacts
  "Makes a sequence of maps representing maven artefacts following the `:maven.deploy/artefact` spec.

  Here representations for a pom.xml and a jar are made."
  [{pom-path ::maven.pom/path
    jar-path ::build.jar/path}]
  [{::maven.deploy.artefact/path      pom-path
    ::maven.deploy.artefact/extension "pom"}

   {::maven.deploy.artefact/path jar-path
    ::maven.deploy.artefact/extension "jar"}])

(u/spec-op make-usual-artefacts
           :param {:req [::maven.pom/path
                         ::build.jar/path]}
           :ret ::maven.deploy/artefacts)


(defn make-usual-artefacts+signatures!
  "Make the usual artefacts using [[fr.jeremyschoffen.mbt.alpha.default.maven.common/make-usual-artefacts]] and
  their gpg signatures."
  [ctxt]
  (let [artefacts (make-usual-artefacts ctxt)
        signatures (mbt-core/maven-sign-artefacts!
                     (assoc ctxt ::maven.deploy/artefacts artefacts))]
    (into artefacts signatures)))

(u/spec-op make-usual-artefacts+signatures!
           :deps [make-usual-artefacts mbt-core/maven-sign-artefacts!]
           :param {:req [::build.jar/path
                         ::maven.pom/path]
                   :opt [::gpg/command
                         ::gpg/home-dir
                         ::gpg/key-id
                         ::gpg/pass-phrase
                         ::gpg/version
                         ::project/working-dir]}
           :ret ::maven.deploy/artefacts)
