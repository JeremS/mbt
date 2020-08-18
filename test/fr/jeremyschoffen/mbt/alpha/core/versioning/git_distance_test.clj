(ns fr.jeremyschoffen.mbt.alpha.core.versioning.git-distance-test
  (:require
    [clojure.spec.test.alpha :as st]
    [clojure.test :refer [deftest testing]]
    [testit.core :refer :all]
    [cognitect.anomalies :as anom]

    [fr.jeremyschoffen.mbt.alpha.core.versioning.git-distance :as sv]))

(st/instrument `[sv/git-distance-version
                 sv/bump])

(def dumy-sha "AAA123")

(deftest simple-version
  (let [base {:number 0
              :distance 0
              :sha dumy-sha
              :dirty? true}]
    (facts
      (-> base sv/git-distance-version str) => "0-DIRTY"

      (-> base
          (assoc :dirty? false)
          sv/git-distance-version
          str) => (str "0")

      (-> base
          (assoc :distance 5)
          sv/git-distance-version
          str) => (str "0-5-g" dumy-sha "-DIRTY"))))



(deftest bump
  (let [base {:number 0
              :distance 0
              :sha dumy-sha
              :dirty? false}]
    (facts
      (-> base sv/git-distance-version sv/bump str)
      =throws=> (ex-info? "Duplicating tag."
                          {::anom/category ::anom/forbidden
                           :mbt/error :versioning/duplicating-tag})

      (-> base
          (assoc :distance 5)
          sv/git-distance-version
          sv/bump
          str)
      => (str "5"))))
