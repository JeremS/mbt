(ns com.jeremyschoffen.mbt.alpha.default.names-test
  (:require
    [clojure.test :refer [deftest testing]]
    [clojure.spec.test.alpha :as st]
    [testit.core :refer :all]
    [com.jeremyschoffen.java.nio.file :as fs]

    [com.jeremyschoffen.mbt.alpha.default.defaults :as defaults]
    [com.jeremyschoffen.mbt.alpha.core.utils :as u]
    [com.jeremyschoffen.mbt.alpha.test.helpers :as h]))


(st/instrument)


(deftest names
  (let [repo (h/make-temp-repo!)
        group-id (-> repo fs/file-name str symbol)]
    (facts
      (defaults/group-id {:project/working-dir (u/safer-path repo)})
      => group-id

      (defaults/group-id {:project/working-dir (u/safer-path repo "module1" "toto")})
      => group-id

      (defaults/artefact-name {:project/working-dir (u/safer-path repo)})
      => group-id

      (defaults/artefact-name {:project/working-dir (u/safer-path repo "module1" "toto")})
      => 'module1-toto

      (defaults/tag-base-name {:project/working-dir (u/safer-path repo)})
      => (str group-id)

      (defaults/tag-base-name {:project/working-dir (u/safer-path repo "module1" "toto")})
      => "module1-toto")))
