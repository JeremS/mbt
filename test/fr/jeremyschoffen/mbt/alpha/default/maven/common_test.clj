(ns fr.jeremyschoffen.mbt.alpha.default.maven.common-test
  (:require
    [clojure.test :refer :all]
    [clojure.spec.test.alpha :as st]
    [testit.core :refer :all]
    [fr.jeremyschoffen.mbt.alpha.default.maven.common :as maven-common]
    [fr.jeremyschoffen.mbt.alpha.utils :as u]))

(u/pseudo-nss
  project)

(st/instrument `[maven-common/make-github-like-scm-map])

(deftest github-like-scm
  (let [ex1 (maven-common/make-github-like-scm-map
              {::project/git-url "https://toto@gitlab.com/User/Project.git"})
        ex2 (maven-common/make-github-like-scm-map
              {::project/git-url "https://gitlab.com/User/Project.git"})]
    (facts
      ex1 => ex2
      ex1 => #:fr.jeremyschoffen.mbt.alpha.maven.scm{:connection "scm:git:git://gitlab.com/User/Project.git.git",
                                                     :developer-connection "scm:git:ssh://git@gitlab.com/User/Project.git.git",
                                                     :url "https://gitlab.com/User/Project.git"})))
