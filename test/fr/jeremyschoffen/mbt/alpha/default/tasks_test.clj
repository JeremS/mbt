(ns fr.jeremyschoffen.mbt.alpha.default.tasks-test
  (:require
    [clojure.test :refer :all]
    [cognitect.anomalies :as anom]
    [testit.core :refer :all]
    [fr.jeremyschoffen.java.nio.alpha.file :as fs]
    [fr.jeremyschoffen.mbt.alpha.core :as mbt-core]
    [fr.jeremyschoffen.mbt.alpha.default.config :as mbt-config]
    [fr.jeremyschoffen.mbt.alpha.default.tasks :as mbt-tasks]
    [fr.jeremyschoffen.mbt.alpha.test.repos :as test-repos]
    [fr.jeremyschoffen.mbt.alpha.utils :as u]))

(u/pseudo-nss
  project)


(def basic-conf {::project/working-dir test-repos/tasks-test-repo})

(def conf (into (sorted-map) (mbt-config/make-base-config basic-conf)))



(deftest guard-working
  (fact
    (mbt-tasks/jar! conf) =throws=> (ex-info? "Can't build a skinny jar while having non maven deps."
                                              {::anom/category ::anom/forbidden
                                               :mbt/error :invalid-deps
                                               :faulty-deps '#{subrepo/subrepo}})))
