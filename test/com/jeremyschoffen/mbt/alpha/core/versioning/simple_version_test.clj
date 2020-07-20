(ns com.jeremyschoffen.mbt.alpha.core.versioning.simple-version-test
  (:require
    [clojure.spec.test.alpha :as st]
    [clojure.test :refer [deftest testing]]
    [testit.core :refer :all]
    [cognitect.anomalies :as anom]

    [com.jeremyschoffen.mbt.alpha.core.versioning.simple-version :as sv]))

(st/instrument [sv/simple-version
                sv/bump])

(def dumy-sha "AAA123")

(deftest simple-version
  (let [base {:number 0
              :distance 0
              :sha dumy-sha
              :dirty? true}]
    (facts
      (-> base sv/simple-version str) => "0-DIRTY"

      (-> base
          (assoc :dirty? false)
          sv/simple-version
          str) => (str "0")

      (-> base
          (assoc :distance 5)
          sv/simple-version
          str) => (str "0-5-g" dumy-sha "-DIRTY"))))


(deftest bump
  (let [base {:number 0
              :distance 0
              :sha dumy-sha
              :dirty? false}]
    (facts
      (-> base sv/simple-version sv/bump str)
      =throws=> (ex-info? "Duplicating tag."
                          {::anom/category ::anom/forbidden
                           :mbt/error :versioning/duplicating-tag})

      (-> base
          (assoc :distance 5)
          sv/simple-version
          sv/bump
          str) => (str "5"))))
