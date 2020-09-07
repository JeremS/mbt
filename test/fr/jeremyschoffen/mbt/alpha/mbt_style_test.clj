(ns fr.jeremyschoffen.mbt.alpha.mbt-style-test
  (:require
    [clojure.edn :as edn]
    [clojure.spec.test.alpha :as st]
    [clojure.test :refer :all]
    [testit.core :refer :all]
    [fr.jeremyschoffen.java.nio.alpha.file :as fs]

    [fr.jeremyschoffen.mbt.alpha.core :as mbt-core]
    [fr.jeremyschoffen.mbt.alpha.default :as mbt-default]
    [fr.jeremyschoffen.mbt.alpha.mbt-style :as mbt-build]
    [fr.jeremyschoffen.mbt.alpha.utils :as u]
    [fr.jeremyschoffen.mbt.alpha.test.helpers :as h]))




(u/pseudo-nss
  git
  git.commit
  maven
  maven.scm
  project
  versioning
  version-file)

(st/instrument `[mbt-core/git-commit!
                 mbt-core/git-add-all!
                 mbt-default/versioning-last-version
                 mbt-default/versioning-get-tag
                 mbt-build/bump-project!
                 mbt-build/update-scm-tag])


(def project-name "mbt-style")
(def repo (h/make-temp-repo!))
(def version-file-path (u/safer-path repo "src" "version.clj"))
(def doc-path (u/safer-path repo "doc"))

(spit (u/safer-path repo "deps.edn") "{}")
(def conf (->> {::git/repo repo
                ::project/working-dir (u/safer-path repo)
                ::project/name project-name
                ::maven/group-id 'group
                ::project/git-url "https://github.com/User/project"
                ::version-file/path version-file-path
                ::version-file/ns 'version
                ::versioning/scheme mbt-default/git-distance-scheme}
               mbt-default/config
               (into (sorted-map))))

(defn spit-doc! [conf]
  (-> conf
      (select-keys [::project/maven-coords
                    ::project/git-coords])
      pr-str
      (->> (spit doc-path))))


(defn generate-docs! [conf]
  (-> conf
      (u/assoc-computed ::versioning/version mbt-default/versioning-last-version
                        ::project/version mbt-default/versioning-project-version
                        ::project/maven-coords mbt-default/deps-make-maven-coords
                        ::project/git-coords mbt-default/deps-make-git-coords)
      (assoc-in [::git/commit! ::git.commit/message] "Generated the docs.")
      (mbt-default/generate-then-commit! (u/do-side-effect! spit-doc!))))


;;----------------------------------------------------------------------------------------------------------------------
;; make the base project
;;----------------------------------------------------------------------------------------------------------------------
(mbt-core/git-add-all! conf)

(def commit-after-adding-deps (-> conf
                                  (assoc-in [::git/commit! ::git.commit/message] "Commiting deps file.")
                                  mbt-core/git-commit!))
(def recovered-commit-after-adding-deps (mbt-core/git-last-commit conf))




;;----------------------------------------------------------------------------------------------------------------------
;; First bump
;;----------------------------------------------------------------------------------------------------------------------
(mbt-build/bump-project! conf)

(def commit-after-first-bump (mbt-core/git-last-commit conf))
(def last-version-after-first-bump (mbt-default/versioning-last-version conf))
(def last-tag-after-first-bump (-> conf
                                   (assoc ::versioning/version last-version-after-first-bump)
                                   mbt-default/versioning-get-tag))


;;----------------------------------------------------------------------------------------------------------------------
;; First docs
;;----------------------------------------------------------------------------------------------------------------------
(generate-docs! conf)

(def commit-after-first-doc (mbt-core/git-last-commit conf))
(def first-bump-version (-> conf
                            mbt-default/versioning-last-version
                            str))
(def doc-after-first-bump (-> doc-path slurp edn/read-string))


;;----------------------------------------------------------------------------------------------------------------------
;; second bump
;;----------------------------------------------------------------------------------------------------------------------
(mbt-build/bump-project! conf)

(def commit-after-second-bump (mbt-core/git-last-commit conf))
(def last-version-after-second-bump (mbt-default/versioning-last-version conf))
(def last-tag-after-second-bump (-> conf
                                    (assoc ::versioning/version last-version-after-second-bump)
                                    mbt-default/versioning-get-tag))




;;----------------------------------------------------------------------------------------------------------------------
;; Second docs
;;----------------------------------------------------------------------------------------------------------------------
(generate-docs! conf)

(def commit-after-second-doc (mbt-core/git-last-commit conf))
(def second-bump-version (-> conf
                             mbt-default/versioning-last-version
                             str))
(def doc-after-second-bump (-> doc-path slurp edn/read-string))




;;----------------------------------------------------------------------------------------------------------------------
;; Tests
;;----------------------------------------------------------------------------------------------------------------------
(defn test-after-bump [previous-commit
                       commit-at-bump
                       tag-at-bump
                       version-at-bump]
  (facts
    (::git.commit/name commit-at-bump)
    =not=> (::git.commit/name previous-commit)

    (::git.commit/message commit-at-bump)
    => "Bump project - Added version file."

    (::git.commit/name commit-at-bump)
    => (::git.commit/name tag-at-bump)

    (-> conf
        (assoc ::versioning/version version-at-bump)
        mbt-build/update-scm-tag
        ::maven/scm
        ::maven.scm/tag)
    => (::git.commit/name tag-at-bump)))


(defn test-after-generating-docs [commit-at-last-bump
                                  commit-at-docs
                                  version-at-last-bump
                                  expected-version
                                  current-doc]
  (facts
    commit-at-last-bump
    =not=> commit-at-docs

    (::git.commit/message commit-at-docs)
    => "Generated the docs."

    version-at-last-bump
    => expected-version

    (get-in current-doc [::project/maven-coords 'group/mbt-style])
    => #:mvn{:version expected-version}

    (get-in current-doc [::project/git-coords 'group/mbt-style])
    => {:git/url "https://github.com/User/project"
        :sha (::git.commit/name commit-at-last-bump)}))

(deftest scenario
  (testing "repo properly initialized"
    (fact commit-after-adding-deps => recovered-commit-after-adding-deps))

  (testing "Base git scm valid"
    (fact
      (::maven/scm conf) => #:fr.jeremyschoffen.mbt.alpha.maven.scm{:connection "scm:git:git://github.com/User/project.git",
                                                                    :developer-connection "scm:git:ssh://git@github.com/User/project.git",
                                                                    :url "https://github.com/User/project"}))

  (testing "After first bump"
    (test-after-bump commit-after-adding-deps
                     commit-after-first-bump
                     last-tag-after-first-bump
                     last-version-after-first-bump))

  (testing "After generating docs the first time"
    (test-after-generating-docs commit-after-first-bump
                                commit-after-first-doc
                                first-bump-version "0"
                                doc-after-first-bump))

  (testing "After second bump"
    (test-after-bump commit-after-first-doc
                     commit-after-second-bump
                     last-tag-after-second-bump
                     last-version-after-second-bump))


  (testing "After generating docs the second time"
    (test-after-generating-docs commit-after-second-bump
                                commit-after-second-doc
                                second-bump-version "2"
                                doc-after-second-bump)))
