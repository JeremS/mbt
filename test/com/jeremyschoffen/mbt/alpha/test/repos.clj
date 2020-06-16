(ns com.jeremyschoffen.mbt.alpha.test.repos
  (:require
    [com.jeremyschoffen.mbt.alpha.core.building.deps :as deps]
    [com.jeremyschoffen.mbt.alpha.core.utils :as u]))


(def test-repos (u/safer-path "test-repos"))

(def deploy-project (u/safer-path test-repos "deploy"))

(def monorepo (u/safer-path test-repos "monorepo"))
(def monorepo-p1 (u/safer-path monorepo "project1"))
(def monorepo-p2 (u/safer-path monorepo "project2"))

(def monorepo-project1-deps
  (-> {:project/working-dir monorepo-p1}
      (deps/get-deps)
      (assoc-in [:deps 'project2/project2 :local/root] (str monorepo-p2))))