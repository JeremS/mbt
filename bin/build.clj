(ns build
  (:require
    [clojure.spec.test.alpha :as spec-test]
    [fr.jeremyschoffen.mbt.alpha.core :as mbt-core]
    [fr.jeremyschoffen.mbt.alpha.default :as mbt-defaults]
    [fr.jeremyschoffen.mbt.alpha.utils :as u]))


(spec-test/instrument
  `[mbt-core/deps-make-coord

    mbt-defaults/write-version-file!
    mbt-defaults/generate-before-bump!
    mbt-defaults/bump-tag!
    mbt-defaults/build-jar!
    mbt-defaults/install!])

(def specific-conf
  {:maven/group-id 'fr.jeremyschoffen
   :project/author "Jeremy Schoffen"
   :version-file/ns 'fr.jeremyschoffen.mbt.alpha.version
   :version-file/path (u/safer-path "src" "main" "fr" "jeremyschoffen" "mbt" "alpha" "version.clj")
   :versioning/scheme mbt-defaults/simple-scheme
   :versioning/major :alpha})


(def conf (->> specific-conf
               mbt-defaults/make-conf
               (into (sorted-map))))


(defn generate-docs! [conf]
  (println "building the docs!"))


(defn new-milestone! [param]
  (-> param
      (mbt-defaults/generate-before-bump! (u/side-effect! generate-docs!)
                                          (u/side-effect! mbt-defaults/write-version-file!))
      (u/side-effect! mbt-defaults/bump-tag!)))



(comment
  (mbt-defaults/generate-before-bump! conf
                                      (u/side-effect! generate-docs!)
                                      (u/side-effect! mbt-defaults/write-version-file!))

  (new-milestone! conf)

  (mbt-core/clean! conf)

  (str (mbt-defaults/anticipated-next-version conf))

  (mbt-defaults/build-jar! conf)
  (mbt-defaults/install! conf)

  (-> conf
      (assoc :project/version "0")
      (u/side-effect! mbt-defaults/build-jar!)
      mbt-defaults/install!)

  (-> conf
      (assoc :maven/server mbt-defaults/clojars)
      mbt-defaults/deploy!))



