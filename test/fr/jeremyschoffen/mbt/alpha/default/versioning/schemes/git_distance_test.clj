(ns fr.jeremyschoffen.mbt.alpha.default.versioning.schemes.git-distance-test
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


(def git-distance-ctxt {::versioning/scheme vs/git-distance-scheme})
(def git-distance-ctxt-alpha {::versioning/scheme vs/git-distance-scheme
                              ::versioning/bump-level :alpha})
(def git-distance-ctxt-beta {::versioning/scheme vs/git-distance-scheme
                             ::versioning/bump-level :beta})


(def simple-init-str (str (vs/initial-version git-distance-ctxt)))
(def simple-init-alpha-str (str (vs/initial-version git-distance-ctxt-alpha)))
(def simple-init-beta-str (str (vs/initial-version git-distance-ctxt-beta)))


(deftest initial-version
  (facts
    simple-init-str => "0"
    simple-init-alpha-str => "0-alpha"))



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
  (testing "Basic"
    (facts
      (-> git-distance-ctxt
          (assoc ::git/description (make-dumy-desc simple-init-str 0 true))
          current-version)

      => (str simple-init-str "-DIRTY")

      (-> git-distance-ctxt
          (assoc ::git/description (make-dumy-desc simple-init-str dumy-dist true))
          current-version)
      => (str simple-init-str "-" dumy-dist "-g" dumy-sha "-DIRTY")))

  (testing "alpha"
    (facts
      (-> git-distance-ctxt-alpha
          (assoc ::git/description (make-dumy-desc simple-init-alpha-str 0 true))
          current-version)
      => (str simple-init-str "-alpha-DIRTY")

      (-> git-distance-ctxt-alpha
          (assoc ::git/description (make-dumy-desc simple-init-alpha-str dumy-dist true))
          current-version)
      => (str simple-init-str "-alpha-" dumy-dist "-g" dumy-sha "-DIRTY"))))



(deftest error-cases-versioning
  (testing "Duplicating"
    (facts
      (-> git-distance-ctxt
          (assoc ::git/description (make-dumy-desc simple-init-str 0 false))
          (u/assoc-computed ::versioning/version vs/current-version)
          vs/bump)
      =throws=> (ex-info? identity
                          {::anom/category ::anom/forbidden
                           :mbt/error :versioning/duplicating-tag})

      (-> git-distance-ctxt-alpha
          (assoc ::git/description (make-dumy-desc simple-init-alpha-str 0 false))
          (u/assoc-computed ::versioning/version vs/current-version)
          vs/bump)
      =throws=> (ex-info? identity
                          {::anom/category ::anom/forbidden
                           :mbt/error :versioning/duplicating-tag})))

  (testing "Going backward"
    (facts
      (-> git-distance-ctxt
          (assoc ::git/description (make-dumy-desc simple-init-str dumy-dist false)
                 ::versioning/bump-level :beta)
          (u/assoc-computed ::versioning/version vs/current-version)
          vs/bump)
      =throws=> (ex-info? identity
                          {::anom/category ::anom/forbidden
                           :mbt/error :versioning/going-backward})

      (-> git-distance-ctxt-beta
          (assoc ::git/description (make-dumy-desc simple-init-beta-str dumy-dist false)
                 ::versioning/bump-level :alpha)
          (u/assoc-computed ::versioning/version vs/current-version)
          vs/bump)
      =throws=> (ex-info? identity
                          {::anom/category ::anom/forbidden
                           :mbt/error :versioning/going-backward}))))

(deftest basic-versioning
  (testing "stable -> stable"
    (facts
      (-> git-distance-ctxt
          (assoc ::git/description (make-dumy-desc simple-init-str dumy-dist false))
          (u/assoc-computed ::versioning/version vs/current-version)
          vs/bump
          str)
      => (str dumy-dist)))

  (testing "alpha -> alpha"
    (facts
      (-> git-distance-ctxt-alpha
          (assoc ::git/description (make-dumy-desc simple-init-alpha-str dumy-dist false))
          (u/assoc-computed ::versioning/version vs/current-version)
          vs/bump
          str)
      => (str dumy-dist "-alpha")))

  (testing "alpha -> stable"
    (facts
      (-> git-distance-ctxt-alpha
          (assoc ::git/description (make-dumy-desc simple-init-alpha-str dumy-dist false)
                 ::versioning/bump-level :stable)
          (u/assoc-computed ::versioning/version vs/current-version)
          vs/bump
          str)
      => (str dumy-dist)))

  (testing "alpha -> beta"
    (facts
      (-> git-distance-ctxt-alpha
          (assoc ::git/description (make-dumy-desc simple-init-alpha-str dumy-dist false)
                 ::versioning/bump-level :beta)
          (u/assoc-computed ::versioning/version vs/current-version)
          vs/bump
          str)
      => (str dumy-dist "-beta"))))
