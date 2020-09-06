(ns fr.jeremyschoffen.mbt.alpha.default.deps-test
  (:require
    [clojure.test :refer [deftest testing]]
    [clojure.spec.test.alpha :as stest]
    [clojure.tools.deps.alpha.util.maven :as deps-maven]
    [testit.core :refer :all]

    [fr.jeremyschoffen.mbt.alpha.default.deps :as deps]
    [fr.jeremyschoffen.mbt.alpha.utils :as u]
    [fr.jeremyschoffen.mbt.alpha.test.helpers :as h]
    [fr.jeremyschoffen.mbt.alpha.core :as mbt-core]
    [clojure.spec.test.alpha :as st]))


(u/pseudo-nss
  git
  git.commit
  git.tag
  maven
  project
  versioning)


(stest/instrument `[deps/make-maven-deps-coords
                    deps/make-git-deps-coords])

(def group-id 'group)
(def project-name 'project-gamma)
(def classifier 'sources)
(def version "0.4.3-beta")

(def conf
  {::maven/group-id      group-id
   ::maven/artefact-name project-name
   ::maven/classifier    classifier
   ::project/version     version})

;;----------------------------------------------------------------------------------------------------------------------
;; Maven
;;----------------------------------------------------------------------------------------------------------------------
(def ex (deps/make-maven-deps-coords conf))

(deftest make-maven-deps-coords
  (facts
    ex => {'group/project-gamma$sources {:mvn/version version}}

    (str (deps-maven/coord->artifact 'group/project-gamma$sources {:mvn/version version}))
    => (clojure.string/join ":" [group-id project-name "jar" classifier version])))


;;----------------------------------------------------------------------------------------------------------------------
;; Git
;;----------------------------------------------------------------------------------------------------------------------
(comment)

(deftest make-git-deps-coords
  (let [repo (h/make-temp-repo!)
        ctxt (assoc conf
               ::project/git-url "http://www.dummyurl.com"
               ::git/repo repo
               ::versioning/tag-base-name "tag"
               ::versioning/version "1")]

    (mbt-core/git-tag!
      {::git/repo repo
       ::git/tag! {::git.tag/name  "tag-v1"
                   ::git.tag/message ""}})

    (fact (deps/make-git-deps-coords ctxt)
          => {'group/project-gamma$sources {:git/url "http://www.dummyurl.com",
                                            :sha (-> ctxt
                                                     (assoc ::git.tag/name "tag-v1")
                                                     mbt-core/git-get-tag
                                                     ::git.commit/name)}})))
