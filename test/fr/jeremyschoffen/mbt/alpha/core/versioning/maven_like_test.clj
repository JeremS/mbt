(ns fr.jeremyschoffen.mbt.alpha.core.versioning.maven-like-test
  (:require
    [clojure.test :refer [deftest testing]]
    [clojure.spec.test.alpha :as st]
    [testit.core :refer :all]
    [cognitect.anomalies :as anom]

    [fr.jeremyschoffen.mbt.alpha.core.versioning.maven-like :as version]))


(st/instrument [version/parse-version
                version/maven-version
                version/semver-version
                version/bump
                version/safer-bump])

(deftest parse-version
  (facts
    (version/parse-version "1.1.2-alpha")
    => {:subversions [1 1 2], :qualifier {:label :alpha :n 1}}

    (version/parse-version "1.1.2-alpha1-0x123ABC-DIRTY")
    => {:subversions [1 1 2], :qualifier {:label :alpha :n 1}}))


(deftest map->maven-version
  (let [base-version "1.1.2-alpha2"
        snapshot "1.1.2-SNAPSHOT"
        weird  "1.1.2-weird"
        distance 3
        sha "AAA123"
        git-part {:distance distance
                  :sha sha
                  :dirty? true}]
    (st/unstrument `version/maven-version)
    (facts
      (-> base-version
          version/parse-version
          (merge git-part)
          version/maven-version
          str)
      => (str base-version "-" distance "-g" sha "-DIRTY")

      (-> snapshot
          version/parse-version
          (merge git-part)
          version/maven-version
          str)
      =throws=> (ex-info? identity
                          {::anom/category ::anom/unsupported})

      (-> weird
          version/parse-version
          (merge git-part)
          version/maven-version
          str)
      =throws=> (ex-info? identity
                          {::anom/category ::anom/unsupported})))
  (st/instrument `version/maven-version))


(deftest map->semver-version
  (let [distance 3
        sha "AAA123"
        git-part {:distance distance
                  :sha sha
                  :dirty? true}]

    (facts
      (-> "1.1.2-alpha2"
          version/parse-version
          (merge git-part)
          (version/semver-version)
          str)
      => (str "1.1.2-" distance "-g" sha "-DIRTY"))))


(defn str->semver [s]
  (-> s
      version/parse-version
      version/semver-version))


(defn str->maven [s]
  (-> s
      version/parse-version
      version/maven-version))


(deftest version-comparison
  (testing "Basic semver versioning"
    (let [v1 (str->semver "1.1.1")
          v1' (str->semver "1.1.1")
          v2 (str->semver "1.1.2")]
      (facts
        (compare v1 v1') => zero?
        (compare v1 v2) => neg?
        (compare v2 v1) => pos?)))

  (testing "Basic maven versioning."
    (let [v1 (str->maven "1.1.1")
          v1' (str->maven "1.1.1")
          v2 (str->maven "1.1.2")]
      (facts
        (compare v1 v1') => zero?
        (compare v1 v2) => neg?
        (compare v2 v1) => pos?)))

  (testing "Maven versioning with qualifiers."
    (let [v1 (str->maven "1.1.1-alpha")
          v1' (str->maven "1.1.1-alpha")
          v2 (str->maven "1.1.2-alpha3")
          v3 (str->maven "1.1.2-beta4")
          v4 (str->maven "1.1.2-rc")
          v5 (str->maven "1.1.2")]
      (facts
        (compare v1 v1') => zero?
        (compare v1 v2) => neg?
        (compare v2 v3) => neg?
        (compare v3 v4) => neg?
        (compare v4 v5) => neg?
        (compare v2 v1) => pos?
        (compare v4 v2) => pos?))))


(defn unsafe-bump [v & bumps]
  (reduce
    (fn [v b]
      (version/bump v b))
    v
    bumps))


(def safer-bump version/safer-bump)


(defn semver-like-progression [v]
  (let [v* (atom v)
        bump! (fn [& bumps]
                (swap! v* #(apply unsafe-bump % bumps)))]
    (bump! :patch :patch :patch)
    (fact (str @v*) => "0.1.3")

    (bump! :patch :patch :patch)
    (fact (str @v*) => "0.1.6")

    (bump! :major :minor :patch)
    (fact (str @v*) => "1.1.1")

    (bump! :patch :minor)
    (fact (str @v*) => "1.2.0")

    (bump! :patch :major)
    (fact (str @v*) => "2.0.0")))


(deftest semver-like-bumps
  (semver-like-progression version/initial-semver-version)
  (semver-like-progression version/initial-maven-version))


(deftest maven-like-bumps
  (let [v* (atom version/initial-maven-version)
        bump! (fn [& bumps]
                (swap! v* #(apply unsafe-bump % bumps)))]

    (bump! :patch :patch :patch :alpha)
    (fact (str @v*) => "0.1.4-alpha")

    (bump! :alpha)
    (fact (str @v*) => "0.1.4-alpha2")

    (bump! :beta :beta :beta)
    (fact (str @v*) => "0.1.4-beta3")

    (bump! :patch)
    (fact (str @v*) => "0.1.5")

    (bump! :rc)
    (fact (str @v*) => "0.1.6-rc")

    (bump! :minor)
    (fact (str @v*) => "0.2.0")

    (bump! :rc)
    (fact (str @v*) => "0.2.1-rc")

    (bump! :major)
    (fact (str @v*) => "1.0.0")))


(defn test-safer-version-progression [version-cstr]
  (let [make-version (fn [s]
                       (-> s
                           version-cstr
                           (assoc :distance 0)))]
    (testing "Every case here would duplicate versions"
      (let [v (make-version "0.0.0")]
        (facts
          (str (unsafe-bump v :patch))       => "0.0.1"
          (str (safer-bump v :patch)) =throws=> Exception

          (str (unsafe-bump v :minor))       => "0.1.0"
          (str (safer-bump v :minor)) =throws=> Exception

          (str (unsafe-bump v :major))       => "1.0.0"
          (str (safer-bump v :major)) =throws=> Exception)))

    (testing "Only patch would duplicate versions"
      (let [v (make-version "0.0.1")]
        (facts
          (str (unsafe-bump v :patch))       => "0.0.2"
          (str (safer-bump v :patch)) =throws=> Exception

          (str (unsafe-bump v :minor)) => "0.1.0"
          (str (safer-bump v :minor))  => "0.1.0"

          (str (unsafe-bump v :major)) => "1.0.0"
          (str (safer-bump v :major))  => "1.0.0")))

    (testing "Would duplicate version up to minor"
      (let [v (make-version "0.1.0")]
        (facts
          (str (unsafe-bump v :patch))       => "0.1.1"
          (str (safer-bump v :patch)) =throws=> Exception

          (str (unsafe-bump v :minor))       => "0.2.0"
          (str (safer-bump v :minor)) =throws=> Exception

          (str (unsafe-bump v :major)) => "1.0.0"
          (str (safer-bump v :major))  => "1.0.0")))))


(deftest test-safer-bumps-semver-like
  (test-safer-version-progression str->semver)
  (test-safer-version-progression str->maven))

(defn str->maven+git [s]
  (-> s
      str->maven
      (assoc :distance 0)))

(deftest test-ordering-in-maven-specifics
  (let [v (-> "0.1.0" str->maven+git)]
    (fact
      (safer-bump v :beta)
      =throws=>
      (ex-info? identity {::anom/category ::anom/forbidden
                          :mbt/error :versioning/duplicating-tag})))

  (let [v (-> "0.1.0-beta" str->maven+git)]
    (facts
      (str (unsafe-bump v :alpha)) => "0.1.0-alpha"

      (str (safer-bump v :alpha))
      =throws=>
      (ex-info? identity {::anom/category ::anom/forbidden
                          :mbt/error :versioning/duplicating-tag})

      (-> v
          (assoc :distance 1)
          (safer-bump :alpha)
          str)
      =throws=>
      (ex-info? identity {::anom/category ::anom/forbidden
                          :mbt/error :versioning/going-backward})))

  (let [v (-> "0.1.0-beta"
              str->maven+git)]
    (facts
      (str (unsafe-bump v :patch)) => "0.1.1"
      (str (unsafe-bump v :beta)) => "0.1.0-beta2"
      (str (safer-bump v :beta))
      =throws=>
      (ex-info? identity {::anom/category ::anom/forbidden
                          :mbt/error :versioning/duplicating-tag}))))
