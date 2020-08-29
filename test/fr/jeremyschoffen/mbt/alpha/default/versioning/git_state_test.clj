(ns fr.jeremyschoffen.mbt.alpha.default.versioning.git-state-test
  (:require
    [clojure.test :refer [deftest testing]]
    [clojure.spec.test.alpha :as st]
    [fr.jeremyschoffen.java.nio.alpha.file :as fs]
    [cognitect.anomalies :as anom]
    [testit.core :refer :all]

    [fr.jeremyschoffen.mbt.alpha.core :as mbt-core]
    [fr.jeremyschoffen.mbt.alpha.default.specs]
    [fr.jeremyschoffen.mbt.alpha.default.versioning.git-state :as git-state]
    [fr.jeremyschoffen.mbt.alpha.default.config :as config]
    [fr.jeremyschoffen.mbt.alpha.default.versioning.schemes.protocols :as vp]
    [fr.jeremyschoffen.mbt.alpha.test.helpers :as h]

    [fr.jeremyschoffen.mbt.alpha.utils :as u]))


(u/pseudo-nss
  git
  git.commit
  git.describe
  git.tag
  project
  project.deps
  versioning)


(st/instrument `[mbt-core/git-add!
                 mbt-core/git-add-all!
                 mbt-core/git-commit!
                 mbt-core/git-tag!

                 git-state/most-recent-description
                 git-state/current-version
                 git-state/next-version
                 git-state/next-tag
                 git-state/bump-tag!])



(defn novelty! [repo & dirs]
  (apply h/add-src! repo dirs)
  (mbt-core/git-add-all! {::git/repo repo})
  (mbt-core/git-commit! {::git/repo repo
                         ::git/commit! {::git.commit/message "Novelty!"}}))


(deftest most-recent-desc
  (let [repo (h/make-temp-repo!)
        ctxt {::git/repo repo}]

    (mbt-core/git-tag! (assoc ctxt ::git/tag! {::git.tag/name "tag-v1"
                                               ::git.tag/message ""}))


    (novelty! repo "src")

    (mbt-core/git-tag! (assoc ctxt ::git/tag! {::git.tag/name "tag-v2"
                                               ::git.tag/message ""}))

    (novelty! repo "src")

    (mbt-core/git-tag! (assoc ctxt ::git/tag! {::git.tag/name "gat-v1"
                                               ::git.tag/message ""}))
    (testing "The most recent tag surfaces without pattern to match"
      (fact
        (git-state/most-recent-description ctxt)
        =in=> {::git.describe/distance 0
               ::git/tag {::git.tag/name "gat-v1"}}))

    (testing "The most recent tag with tag name `tag*` surfaces"
      (fact
        (git-state/most-recent-description (assoc ctxt
                                             ::versioning/tag-base-name "tag"))
        =in=> {::git.describe/distance 1
               ::git/tag {::git.tag/name "tag-v2"}}))))


(def tag-name-regex #"(.*)-v(\d.*)")


(defn current-version* [desc]
  (-> desc
      (get-in [::git/tag ::git.tag/name])
      (->> (re-matches tag-name-regex))
      (get 2)))


(defn bump* [v]
  (-> v Integer/parseInt inc str))


(def initial-v "0")


(def test-scheme
  (reify vp/VersionScheme
    (current-version [_ desc]
      (current-version* desc))
    (initial-version [_]
      initial-v)
    (bump [_ v]
      (bump* v))
    (bump [_ v _]
      (bump* v))))


(deftest current-version
  (let [repo (h/make-temp-repo!)
        ctxt {::git/repo repo
              ::versioning/scheme test-scheme}]

    (mbt-core/git-tag! (assoc ctxt
                         ::git/tag! {::git.tag/name "tag-v2"
                                     ::git.tag/message ""}))

    (mbt-core/git-tag! (assoc ctxt
                         ::git/tag! {::git.tag/name "gat-v1.1.1"
                                     ::git.tag/message ""}))

    (facts
      (git-state/current-version ctxt) => "1.1.1"
      (git-state/current-version (assoc ctxt
                                   ::versioning/tag-base-name "tag")) => "2")))


(deftest next-version
  (let [repo (h/make-temp-repo!)
        ctxt {::git/repo repo
              ::versioning/scheme test-scheme}]

    (testing "When no tag exist, current version is nil, next version is initial-v."
      (facts
        (git-state/current-version ctxt) => nil
        (git-state/next-version ctxt) => initial-v))

    (mbt-core/git-tag! (assoc ctxt
                         ::git/tag! {::git.tag/name "p-v3"
                                     ::git.tag/message ""}))

    (testing "When we have a tag, we get current and next version."
      (facts
        (git-state/current-version ctxt) => "3"
        (git-state/next-version ctxt) => "4"))))


(defn make-base-config [user-defined]
  (-> config/base
      (dissoc ::project.deps/file ::project/deps)
      (merge user-defined)
      config/compute-conf))


(deftest next-tag-simple-repo
  (let [repo (h/make-temp-repo!)
        ctxt (make-base-config
               {::git/repo repo
                ::project/working-dir (fs/path repo)
                ::versioning/scheme test-scheme})
        tag (-> ctxt
                (-> git-state/next-tag
                    (update ::git.tag/message clojure.edn/read-string)))
        base-name (::versioning/tag-base-name ctxt)
        tag-name (str base-name
                      "-v"
                      initial-v)]
    (fact
      tag =in=> {::git.tag/name tag-name
                 ::git.tag/message {:name base-name
                                    :version initial-v
                                    :tag-name tag-name
                                    :path "."}})))


(deftest next-tag-mono-repo
  (let [repo (h/make-temp-repo!)
        project-dir1-relative (fs/path "module1" "project1")
        project-dir1 (u/ensure-dir! (fs/path repo project-dir1-relative))

        ctxt1 (make-base-config
                {::git/repo            repo
                 ::project/working-dir project-dir1
                 ::versioning/scheme   test-scheme})
        base-name1 (::versioning/tag-base-name ctxt1)


        project-dir2-relative (fs/path "module1" "project2")
        project-dir2 (u/ensure-dir! (fs/path repo project-dir2-relative))
        ctxt2 (make-base-config
                {::git/repo            repo
                 ::project/working-dir project-dir2
                 ::versioning/scheme   test-scheme})
        base-name2 (::versioning/tag-base-name ctxt2)

        next-tag #(-> %
                      git-state/next-tag
                      (update ::git.tag/message clojure.edn/read-string))]

    (facts
      (next-tag ctxt1)
      =in=> {::git.tag/name    (str base-name1 "-v" initial-v)
             ::git.tag/message {:name    base-name1
                                :version initial-v
                                :path    (str project-dir1-relative)}}

      (next-tag ctxt2)
      =in=> {::git.tag/name    (str base-name2 "-v" initial-v)
             ::git.tag/message {:name    base-name2
                                :version initial-v
                                :path    (str project-dir2-relative)}})))

(deftest bump!
  (let [repo (h/make-uncommited-temp-repo!)
        project-dir1-relative (fs/path "module1" "project1")
        project-dir1 (u/ensure-dir! (fs/path repo project-dir1-relative))

        ctxt1 (make-base-config
                {::git/repo            repo
                 ::project/working-dir project-dir1
                 ::versioning/scheme   test-scheme})

        base-name1 (::versioning/tag-base-name ctxt1)


        project-dir2-relative (fs/path "module1" "project2")
        project-dir2 (u/ensure-dir! (fs/path repo project-dir2-relative))
        ctxt2 (make-base-config
                {::git/repo            repo
                 ::project/working-dir project-dir2
                 ::versioning/scheme   test-scheme})

        base-name2 (::versioning/tag-base-name ctxt2)]

    (facts
      (git-state/bump-tag! ctxt1)
      =throws=> (ex-info? "No commits  found."
                          {::anom/category ::anom/not-found
                           :mbt/error :no-commit})

      (git-state/bump-tag! ctxt2)
      =throws=> (ex-info? "No commits  found."
                          {::anom/category ::anom/not-found
                           :mbt/error :no-commit}))

    (mbt-core/git-commit! (assoc ctxt1
                            ::git/commit! {::git.commit/message "initial commit"}))

    (facts
      (git-state/bump-tag! ctxt1)
      =throws=> (ex-info? "No build file detected."
                          {::anom/category ::anom/not-found
                           :mbt/error :no-build-file})

      (git-state/bump-tag! ctxt2)
      =throws=> (ex-info? "No build file detected."
                          {::anom/category ::anom/not-found
                           :mbt/error      :no-build-file}))

    (h/copy-dummy-deps (fs/path repo project-dir1-relative))
    (h/copy-dummy-deps (fs/path repo project-dir2-relative))

    (facts
      (git-state/bump-tag! ctxt1)
      =throws=> (ex-info? "Can't do this operation on a dirty repo."
                          {::anom/category ::anom/forbidden
                           :mbt/error :dirty-repo})

      (git-state/bump-tag! ctxt2)
      =throws=> (ex-info? "Can't do this operation on a dirty repo."
                          {::anom/category ::anom/forbidden
                           :mbt/error :dirty-repo}))

    (mbt-core/git-add-all! ctxt1)
    (mbt-core/git-commit! (assoc ctxt1
                            ::git/commit! {::git.commit/message "initial commit"}))

    (git-state/bump-tag! ctxt1)
    (git-state/bump-tag! ctxt2)
    (facts
      (git-state/most-recent-description {::git/repo repo
                                          ::versioning/tag-base-name base-name1})
      =in=> {::git/tag {::git.tag/name (str base-name1 "-v" initial-v)}}

      (git-state/most-recent-description {::git/repo repo
                                          ::versioning/tag-base-name base-name2})
      =in=> {::git/tag {::git.tag/name (str base-name2 "-v" initial-v)}})


    (facts
      (git-state/next-tag ctxt1)
      =in=> {::git.tag/name (str base-name1 "-v" (bump* initial-v))}

      (git-state/next-tag ctxt2)
      =in=> {::git.tag/name (str base-name2 "-v" (bump* initial-v))})))
