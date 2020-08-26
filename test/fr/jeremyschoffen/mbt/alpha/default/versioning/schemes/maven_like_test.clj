(ns fr.jeremyschoffen.mbt.alpha.default.versioning.schemes.maven-like-test
  (:require
    [clojure.test :refer [deftest testing]]
    [clojure.spec.test.alpha :as st]
    [cognitect.anomalies :as anom]
    [testit.core :refer :all]

    [fr.jeremyschoffen.mbt.alpha.default.versioning.schemes :as vs]
    [fr.jeremyschoffen.mbt.alpha.utils :as u]))


(u/pseudo-nss
  git
  git.describe
  git.repo
  git.tag
  versioning)


(st/instrument [vs/current-version
                vs/bump
                vs/bump])

(def maven-ctxt {::versioning/scheme vs/maven-scheme})
(def semver-ctxt {::versioning/scheme vs/semver-scheme})


(def maven-init-str (str (vs/initial-version maven-ctxt)))
(def semver-init-str (str (vs/initial-version semver-ctxt)))



(deftest initial-version
  (facts
    maven-init-str => "0.1.0"
    semver-init-str => "0.1.0"))


(def dumy-project-name "project1")
(def dumy-sha "AAA123")
(def dumy-dist 3)

(defn make-dumy-desc [v distance dirty?]
  (let [tag-name (str dumy-project-name "-v" v)]
    {::git/raw-description "dumy raw desc"
     ::git/tag {::git.tag/name tag-name
                ::git.tag/message (pr-str {:name dumy-project-name
                                           :version v})}
     ::git.describe/distance distance
     ::git/sha "AAA123"
     ::git.repo/dirty? dirty?}))

(defn current-version [ctxt]
  (-> ctxt vs/current-version str))

(deftest current-version-test
  (testing "Maven"
    (facts
      (-> maven-ctxt
          (assoc ::git/description (make-dumy-desc maven-init-str 0 true))
          current-version)
      => (str maven-init-str "-DIRTY")

      (-> maven-ctxt
          (assoc ::git/description (make-dumy-desc maven-init-str dumy-dist true))
          current-version)
      => (str maven-init-str "-" dumy-dist "-g" dumy-sha "-DIRTY")))

  (testing "Semver"
    (facts
      (-> semver-ctxt
          (assoc ::git/description (make-dumy-desc semver-init-str 0 false))
          current-version)
      => semver-init-str

      (-> semver-ctxt
          (assoc ::git/description (make-dumy-desc semver-init-str dumy-dist true))
          current-version)
      => (str semver-init-str "-" dumy-dist "-g" dumy-sha "-DIRTY"))))


(deftest error-cases-versioning
  (facts
    (-> maven-ctxt
        (assoc ::git/description (make-dumy-desc maven-init-str 0 false)
               ::versioning/bump-level :patch)
        (u/assoc-computed ::versioning/version vs/current-version)
        vs/bump)
    =throws=> (ex-info? identity
                        {::anom/category ::anom/forbidden
                         :mbt/error :versioning/duplicating-tag})

    (-> semver-ctxt
        (assoc ::git/description (make-dumy-desc semver-init-str 0 false)
               ::versioning/bump-level :patch)
        (u/assoc-computed ::versioning/version vs/current-version)
        vs/bump)
    =throws=> (ex-info? identity
                        {::anom/category ::anom/forbidden
                         :mbt/error :versioning/duplicating-tag})))


(deftest basic-versioning
  (facts
    (-> maven-ctxt
        (assoc ::git/description (make-dumy-desc maven-init-str dumy-dist false)
               ::versioning/bump-level :patch)
        (u/assoc-computed ::versioning/version vs/current-version)
        vs/bump
        str)
    => "0.1.1"

    (-> semver-ctxt
        (assoc ::git/description (make-dumy-desc semver-init-str dumy-dist false)
               ::versioning/bump-level :major)
        (u/assoc-computed ::versioning/version vs/current-version)
        vs/bump
        str)
    => "1.0.0"))
