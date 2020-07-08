(ns build
  (:require
    [clojure.spec.test.alpha :as spec-test]
    [com.jeremyschoffen.mbt.alpha.core :as mbt-core]
    [com.jeremyschoffen.mbt.alpha.default :as mbt-defaults]
    [com.jeremyschoffen.mbt.alpha.utils :as u]))


(spec-test/instrument
  [mbt-defaults/add-version-file!
   mbt-defaults/bump-tag!
   mbt-defaults/build-jar!])

(def specific-conf
  (sorted-map
    :project/working-dir (u/safer-path)
    :versioning/scheme mbt-defaults/simple-scheme
    :versioning/major :alpha
    :project/author "Jeremy Schoffen"
    :version-file/ns 'com.jeremyschoffen.mbt.alpha.version
    :version-file/path (u/safer-path "src" "com" "jeremyschoffen" "mbt" "alpha" "version.clj")))

(def conf (-> specific-conf
              mbt-defaults/make-conf))


(defn new-milestone! [param]
  (-> param
      (u/side-effect! mbt-defaults/add-version-file!)
      (u/side-effect! mbt-defaults/bump-tag!)))


(comment
  (new-milestone! conf)
  (mbt-core/clean! conf)
  (mbt-defaults/build-jar! conf))
