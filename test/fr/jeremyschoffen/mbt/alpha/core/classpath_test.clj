(ns fr.jeremyschoffen.mbt.alpha.core.classpath-test
  (:require
    [clojure.test :refer [deftest]]
    [testit.core :refer :all]
    [fr.jeremyschoffen.java.nio.alpha.file :as fs]
    [fr.jeremyschoffen.mbt.alpha.core.classpath :as cp]
    [fr.jeremyschoffen.mbt.alpha.core.deps :as deps]
    [fr.jeremyschoffen.mbt.alpha.test.repos :as test-repos]
    [fr.jeremyschoffen.mbt.alpha.utils :as u]))


(u/mbt-alpha-pseudo-nss
  classpath
  project
  project.deps)


(def clojure-jar? #(re-matches #".*/clojure-.*\.jar$" %))
(def spec-jar? #(re-matches #".*/core\.specs\.alpha-.*\.jar$" %))
(def specs-jar? #(re-matches #".*/spec\.alpha-.*\.jar$" %))

(def clojure-jars-present?
  (every-pred (partial some clojure-jar?)
              (partial some spec-jar?)
              (partial some specs-jar?)))


(def state-p1 {::project/working-dir  test-repos/monorepo-p1
               ::project/deps test-repos/monorepo-project1-deps})

(def state-p2 (u/assoc-computed {::project/working-dir  test-repos/monorepo-p2
                                 ::project.deps/file (u/safer-path test-repos/monorepo-p2 "deps.edn")}
                                ::project/deps deps/get-deps))

(def cp1 (cp/indexed-classpath state-p1))
(def cp2 (cp/indexed-classpath state-p2))


(deftest classpath-test
  (let []

    (facts
      (-> cp1 ::classpath/jar clojure-jars-present?) => true
      (-> cp2 ::classpath/jar clojure-jars-present?) => false

      (-> cp1 ::classpath/ext-dep set) => #{(str (fs/path test-repos/monorepo-p2  "src"))
                                            (str (fs/path test-repos/monorepo-p2  "resources"))})))
