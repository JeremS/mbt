(ns com.jeremyschoffen.mbt.alpha.core.building.classpath-test
  (:require
    [clojure.test :refer [deftest]]
    [clojure.tools.deps.alpha.reader :as deps-reader]
    [testit.core :refer :all]
    [com.jeremyschoffen.java.nio.file :as fs]
    [com.jeremyschoffen.mbt.alpha.core.building.building-utils :as bu]
    [com.jeremyschoffen.mbt.alpha.core.building.classpath :as cp]
    [com.jeremyschoffen.mbt.alpha.core.building.deps :as deps]
    [com.jeremyschoffen.mbt.alpha.core.helpers.test-repos :as test-repos]
    [com.jeremyschoffen.mbt.alpha.core.utils :as u]))


(def clojure-jar? #(re-matches #".*/clojure-.*\.jar$" %))
(def spec-jar? #(re-matches #".*/core\.specs\.alpha-.*\.jar$" %))
(def specs-jar? #(re-matches #".*/spec\.alpha-.*\.jar$" %))

(def clojure-jars-present?
  (every-pred (partial some clojure-jar?)
              (partial some spec-jar?)
              (partial some specs-jar?)))


(deftest classpath-test
  (let [state-p1 {:project/working-dir  test-repos/monorepo-p1
                  :project/deps test-repos/monorepo-project1-deps}

        state-p2 (u/assoc-computed {:project/working-dir  test-repos/monorepo-p2}
                                   :project/deps deps/get-deps)

        cp1 (cp/indexed-classpath state-p1)
        cp2 (cp/indexed-classpath state-p2)]

    (facts
      (-> cp1 :classpath/jar clojure-jars-present?) => true
      (-> cp2 :classpath/jar clojure-jars-present?) => false

      (-> cp1 :classpath/ext-dep set) => #{(str (fs/path test-repos/monorepo-p2  "src"))
                                           (str (fs/path test-repos/monorepo-p2  "resources"))})))
