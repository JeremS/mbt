(ns com.jeremyschoffen.mbt.alpha.default.versioning.git-state-test
  (:require
    [clojure.test :refer [deftest testing]]
    [clojure.spec.test.alpha :as st]
    [testit.core :refer :all]

    [com.jeremyschoffen.mbt.alpha.core.git :as git]
    [com.jeremyschoffen.mbt.alpha.default.specs :as default-specs]
    [com.jeremyschoffen.mbt.alpha.default.versioning.git-state :as git-state]
    [com.jeremyschoffen.mbt.alpha.default.names :as names]
    [com.jeremyschoffen.mbt.alpha.default.versioning.schemes.protocols :as vp]
    [com.jeremyschoffen.mbt.alpha.test.helpers :as h]

    [com.jeremyschoffen.mbt.alpha.core.utils :as u]
    [com.jeremyschoffen.java.nio.file :as fs]))

(st/instrument)

(defn novelty! [repo & dirs]
  (apply h/add-src! repo dirs)
  (git/add-all! {:git/repo repo})
  (git/commit! {:git/repo repo
                :git/commit! {:git.commit/message "Novelty!"}}))


(deftest most-recent-desc
  (let [repo (h/make-temp-repo!)
        ctxt {:git/repo repo}]

    (git/create-tag! (assoc ctxt :git/tag! {:git.tag/name "tag-v1"
                                            :git.tag/message ""}))


    (novelty! repo "src")

    (git/create-tag! (assoc ctxt :git/tag! {:git.tag/name "tag-v2"
                                            :git.tag/message ""}))

    (novelty! repo "src")

    (git/create-tag! (assoc ctxt :git/tag! {:git.tag/name "gat-v1"
                                            :git.tag/message ""}))
    (testing "The most recent tag surfaces without pattern to match"
      (fact
        (git-state/most-recent-description ctxt)
        =in=> {:git.describe/distance 0
               :git/tag {:git.tag/name "gat-v1"}}))

    (testing "The most recent tag with tag name `tag*` surfaces"
      (fact
        (git-state/most-recent-description (assoc ctxt
                                             :versioning/tag-base-name "tag"))
        =in=> {:git.describe/distance 1
               :git/tag {:git.tag/name "tag-v2"}}))))


(defn current-version* [desc]
  (-> desc
      (get-in [:git/tag :git.tag/name])
      (->> (re-matches default-specs/tag-name-regex))
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
        ctxt {:git/repo repo
              :versioning/scheme test-scheme}]

    (git/create-tag! (assoc ctxt
                       :git/tag! {:git.tag/name "tag-v2"
                                  :git.tag/message ""}))

    (git/create-tag! (assoc ctxt
                       :git/tag! {:git.tag/name "gat-v1.1.1"
                                  :git.tag/message ""}))

    (facts
      (git-state/current-version ctxt) => "1.1.1"
      (git-state/current-version (assoc ctxt
                                   :versioning/tag-base-name "tag")) => "2")))


(deftest next-version
  (let [repo (h/make-temp-repo!)
        ctxt {:git/repo repo
              :versioning/scheme test-scheme}]

    (testing "When no tag exist, current versin is nil, next version is initial-v."
      (facts
        (git-state/current-version ctxt) => nil
        (git-state/next-version ctxt) => initial-v))

    (git/create-tag! (assoc ctxt
                       :git/tag! {:git.tag/name "p-v3"
                                  :git.tag/message ""}))

    (testing "When we have a tag, we get current and next version."
      (facts
        (git-state/current-version ctxt) => "3"
        (git-state/next-version ctxt) => "4"))))


(deftest tag-simple-repo
  (let [repo (h/make-temp-repo!)
        ctxt {:git/repo repo
              :project/working-dir (fs/path repo)
              :versioning/scheme test-scheme}
        tag (-> ctxt
                (u/assoc-computed
                  :git/prefix git/prefix
                  :versioning/tag-base-name names/tag-base-name
                  :versioning/version git-state/next-version)
                git-state/tag
                (update :git.tag/message clojure.edn/read-string))
        base-name (-> repo fs/file-name str)
        tag-name (str base-name
                      "-v"
                      initial-v)]
    (fact
      tag =in=> {:git.tag/name tag-name
                 :git.tag/message {:name base-name
                                   :version initial-v
                                   :tag-name tag-name}})))



