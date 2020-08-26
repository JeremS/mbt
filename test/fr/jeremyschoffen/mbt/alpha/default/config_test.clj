(ns fr.jeremyschoffen.mbt.alpha.default.config-test
  (:require
    [clojure.test :refer [deftest testing]]
    [clojure.spec.test.alpha :as st]
    [testit.core :refer :all]
    [fr.jeremyschoffen.java.nio.alpha.file :as fs]

    [fr.jeremyschoffen.mbt.alpha.default.config :as config]
    [fr.jeremyschoffen.mbt.alpha.test.helpers :as h]
    [fr.jeremyschoffen.mbt.alpha.utils :as u]))


(u/pseudo-nss
  maven
  project
  versioning)



(deftest names
  (let [repo (h/make-temp-repo!)
        wd (u/safer-path repo)
        wd' (u/safer-path repo "module1" "toto")
        _ (u/ensure-dir! wd')

        project-name (-> repo fs/file-name str)
        group-id (symbol project-name)

        ctxt (config/make-base-config {::project/working-dir wd})
        ctxt-alpha (config/make-base-config {::project/working-dir wd
                                             ::versioning/major         :alpha})


        ctxt' (config/make-base-config {::project/working-dir wd'})]
    (testing "Group ids"
      (facts
        (::maven/group-id ctxt)
        => group-id

        (::maven/group-id ctxt')
        => group-id

        (::maven/group-id ctxt-alpha)
        => group-id))


    (testing "Artefact names"
      (facts
        (::maven/artefact-name ctxt)
        => group-id

        (::maven/artefact-name ctxt')
        => 'module1-toto

        (::maven/artefact-name ctxt-alpha)
        => (-> group-id
               (str "-alpha")
               symbol)))


    (testing "Tag names"
      (facts
        (::versioning/tag-base-name ctxt)
        => (str group-id)

        (::versioning/tag-base-name ctxt-alpha)
        => (str group-id "-alpha")


        (::versioning/tag-base-name ctxt')
        => "module1-toto"))))
