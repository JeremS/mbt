(ns com.jeremyschoffen.mbt.alpha.building.classpath-test
  (:require
    [clojure.test :refer [deftest]]
    [clojure.tools.deps.alpha.reader :as deps-reader]
    [testit.core :refer :all]
    [com.jeremyschoffen.java.nio.file :as fs]
    [com.jeremyschoffen.mbt.alpha.building.building-utils :as bu]
    [com.jeremyschoffen.mbt.alpha.building.classpath :as cp]
    [com.jeremyschoffen.mbt.alpha.utils :as u]))


(def clojure-jar? #(re-matches #".*/clojure-.*\.jar$" %))
(def spec-jar? #(re-matches #".*/core\.specs\.alpha-.*\.jar$" %))
(def specs-jar? #(re-matches #".*/spec\.alpha-.*\.jar$" %))

(def clojure-jars-present?
  (every-pred (partial some clojure-jar?)
              (partial some spec-jar?)
              (partial some specs-jar?)))


(deftest classpath-test
  (let [temp-repo (bu/make-test-project)

        state-p1 {:project/working-dir (u/safer-path temp-repo bu/project1)
                  :project/deps (deps-reader/slurp-deps (u/safer-path temp-repo bu/project1 "deps.edn"))}

        state-p2 {:project/working-dir (u/safer-path temp-repo bu/project1)
                  :project/deps (deps-reader/slurp-deps (u/safer-path temp-repo bu/project2 "deps.edn"))}

        cp1 (cp/indexed-classpath state-p1)
        cp2 (cp/indexed-classpath state-p2)]

    (facts
      (-> cp1 :jar clojure-jars-present?) => true
      (-> cp2 :jar clojure-jars-present?) => true

      (-> cp1 :ext-dep first) => (str (fs/path temp-repo bu/project2  "src")))))


