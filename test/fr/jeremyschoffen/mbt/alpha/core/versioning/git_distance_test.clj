(ns fr.jeremyschoffen.mbt.alpha.core.versioning.git-distance-test
  (:require
    [clojure.spec.test.alpha :as st]
    [clojure.test :refer [deftest testing]]
    [testit.core :refer :all]
    [cognitect.anomalies :as anom]

    [fr.jeremyschoffen.mbt.alpha.core.versioning.git-distance :as sv]))

(st/instrument `[sv/simple-version
                 sv/bump])

(def dumy-sha "AAA123")

(deftest simple-version
  (let [base {:number 0
              :distance 0
              :sha dumy-sha
              :dirty? true
              :stable true}
        unstable #(assoc % :stable false)]
    (facts
      (-> base sv/simple-version str) => "0-DIRTY"

      (-> base unstable sv/simple-version str) => "0-unstable-DIRTY"

      (-> base
          (assoc :dirty? false)
          sv/simple-version
          str) => (str "0")

      (-> base
          (assoc :distance 5)
          sv/simple-version
          str) => (str "0-5-g" dumy-sha "-DIRTY")

      (-> base
          (assoc :distance 5)
          unstable
          sv/simple-version
          str) => (str "0-unstable-5-g" dumy-sha "-DIRTY"))))


(deftest bump
  (let [base {:number 0
              :distance 0
              :sha dumy-sha
              :dirty? false
              :stable false}]
    (facts
      (-> base sv/simple-version sv/bump str)
      =throws=> (ex-info? "Duplicating tag."
                          {::anom/category ::anom/forbidden
                           :mbt/error :versioning/duplicating-tag})

      (-> base
          (assoc :distance 5)
          sv/simple-version
          sv/bump
          str)
      => (str "5-unstable")

      (-> base
          (assoc :distance 5)
          sv/simple-version
          (sv/bump :stable)
          str)
      => (str "0")

      (-> base
          (assoc :distance 5
                 :stable true)
          sv/simple-version
          sv/bump
          str)
      => (str "5"))))
