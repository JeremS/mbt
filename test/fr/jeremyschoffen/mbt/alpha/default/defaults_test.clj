(ns fr.jeremyschoffen.mbt.alpha.default.defaults-test
  (:require
    [clojure.test :refer [deftest testing]]
    [clojure.spec.test.alpha :as st]
    [testit.core :refer :all]
    [fr.jeremyschoffen.java.nio.alpha.file :as fs]

    [fr.jeremyschoffen.mbt.alpha.default.defaults :as defaults]
    [fr.jeremyschoffen.mbt.alpha.test.helpers :as h]
    [fr.jeremyschoffen.mbt.alpha.utils :as u]))


(st/instrument [defaults/make-context])

(deftest names
  (let [repo (h/make-temp-repo!)
        wd (u/safer-path repo)
        wd' (u/safer-path repo "module1" "toto")
        _ (u/ensure-dir! wd')

        group-id (-> repo fs/file-name str symbol)

        ctxt (defaults/make-context {:project/working-dir wd})
        ctxt' (defaults/make-context {:project/working-dir wd'})]
    (facts
      (:maven/group-id ctxt)
      => group-id

      (:maven/group-id ctxt')
      => group-id

      (:maven/artefact-name ctxt)
      => group-id

      (:maven/artefact-name ctxt')
      => 'module1-toto

      (:versioning/tag-base-name ctxt)
      => (str group-id)


      (:versioning/tag-base-name ctxt')
      => "module1-toto")))
