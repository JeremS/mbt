(ns fr.jeremyschoffen.mbt.alpha.core.deps-test
  (:require
    [clojure.test :refer [deftest testing]]
    [clojure.spec.test.alpha :as stest]
    [clojure.tools.deps.alpha.util.maven :as deps-maven]
    [testit.core :refer :all]

    [fr.jeremyschoffen.mbt.alpha.core.deps :as deps]
    [fr.jeremyschoffen.mbt.alpha.utils :as u]))

(u/pseudo-nss
  maven
  project)


(stest/instrument `[deps/make-deps-coords])

(def group-id 'group)
(def project-name 'project-gamma)
(def classifier 'sources)
(def version "0.4.3-beta")

(def conf
  {::maven/group-id      group-id
   ::maven/artefact-name project-name
   ::maven/classifier    classifier
   ::project/version     version})


(def ex (deps/make-deps-coords conf))

(deftest make-deps-coords
  (facts
    ex => {'group/project-gamma$sources {:mvn/version version}}

    (str (deps-maven/coord->artifact 'group/project-gamma$sources {:mvn/version version}))
    => (clojure.string/join ":" [group-id project-name "jar" classifier version])))

