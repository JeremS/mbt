(ns fr.jeremyschoffen.mbt.alpha.default.config
  (:require
    [ubergraph.core :as graph]
    [fr.jeremyschoffen.dolly.core :as dolly]
    [fr.jeremyschoffen.mbt.alpha.utils :as u]
    [fr.jeremyschoffen.mbt.alpha.default.config.cleaning :as cleaning-c]
    [fr.jeremyschoffen.mbt.alpha.default.config.compilation :as compi-c]
    [fr.jeremyschoffen.mbt.alpha.default.config.git :as git-c]
    [fr.jeremyschoffen.mbt.alpha.default.config.gpg :as gpg-c]
    [fr.jeremyschoffen.mbt.alpha.default.config.impl :as impl]
    [fr.jeremyschoffen.mbt.alpha.default.config.jar :as jar-c]
    [fr.jeremyschoffen.mbt.alpha.default.config.maven :as maven-c]
    [fr.jeremyschoffen.mbt.alpha.default.config.project :as project-c]
    [fr.jeremyschoffen.mbt.alpha.default.config.versioning :as versioning-conf]))


(dolly/def-clone calc impl/calc)
(dolly/def-clone compute-conf impl/compute)

(defn clone-val
  "Makes a calc using [[fr.jeremyschoffen.mbt.alpha.default.config/calc]]
  to easily dumplicate a value in the conf

  ```clojure
  (calc :k :k)
  ; <=>
  (clone-val :k)
  ```"
  [k]
  (calc #(get % k) k))


(defn pprint-deps
  "Uses ubergraph to pprint our dependency graph."
  [config]
  (-> config
      impl/extract-calcs
      graph/digraph
      graph/pprint))


(def base (merge
            project-c/conf
            cleaning-c/conf
            compi-c/conf
            git-c/conf
            gpg-c/conf2
            versioning-conf/conf
            jar-c/conf
            maven-c/conf))


(defn make-base-config
  ([]
   (make-base-config {}))
  ([user-defined]
   (compute-conf (merge base user-defined))))

