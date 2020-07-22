(ns build
  (:require
    [clojure.spec.test.alpha :as spec-test]
    [com.jeremyschoffen.java.nio.alpha.file :as fs]
    [com.jeremyschoffen.mbt.alpha.core :as mbt-core]
    [com.jeremyschoffen.mbt.alpha.default :as mbt-defaults]
    [com.jeremyschoffen.mbt.alpha.utils :as u]))


(spec-test/instrument
  [mbt-defaults/add-version-file!
   mbt-defaults/bump-tag!
   mbt-defaults/build-jar!
   mbt-defaults/install!])

(def specific-conf
  {:versioning/scheme mbt-defaults/simple-scheme
   :versioning/major :alpha
   :project/author "Jeremy Schoffen"
   :version-file/ns 'com.jeremyschoffen.mbt.alpha.version
   :version-file/path (u/safer-path "src" "com" "jeremyschoffen" "mbt" "alpha" "version.clj")})


(def conf (->> specific-conf
               mbt-defaults/make-conf
               (into (sorted-map))))


(defn new-milestone! [param]
  (-> param
      (u/side-effect! mbt-defaults/add-version-file!)
      (u/side-effect! mbt-defaults/bump-tag!)))


(comment
  (new-milestone! conf)

  (mbt-core/clean! conf)

  (mbt-defaults/build-jar! conf)
  (mbt-defaults/install! conf)

  (-> conf
      (assoc :project/version "0")
      (u/side-effect! mbt-defaults/build-jar!)
      (u/side-effect! mbt-defaults/install!))

  (-> conf
      (assoc :maven/server mbt-defaults/clojars)
      mbt-defaults/deploy!))



