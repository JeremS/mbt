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

(def dirty-base {:number 0
                 :distance     0
                 :sha          dumy-sha
                 :dirty?       true})

(deftest simple-version
  (facts
    (-> dirty-base sv/git-distance-version str) => "0-DIRTY"

    (-> dirty-base
        (assoc :dirty? false)
        sv/git-distance-version
        str) => (str "0")

    (-> dirty-base
        (assoc :distance 5)
        sv/git-distance-version
        str) => (str "0-5-g" dumy-sha "-DIRTY")

    (-> dirty-base
        (assoc :distance 5
               :qualifier :alpha)
        sv/git-distance-version
        str) => (str "0-alpha-5-g" dumy-sha "-DIRTY")))


(def base {:number 0
           :distance 0
           :sha dumy-sha
           :dirty? false})

(deftest bump
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
    => (str "5")

    (-> base
        (assoc :distance 5
               :qualifier :alpha)
        sv/git-distance-version
        sv/bump
        str)
    => (str "5-alpha")

    (-> base
        (assoc :distance 5
               :qualifier :beta)
        sv/git-distance-version
        sv/bump
        str)
    => (str "5-beta")

    (-> base
        (assoc :distance 5
               :qualifier :beta)
        sv/git-distance-version
        (sv/bump :stable)
        str)
    => (str "5")))
