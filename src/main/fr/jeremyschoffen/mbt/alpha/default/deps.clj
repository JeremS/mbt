(ns ^{:author "Jeremy Schoffen"
      :doc "
Utilities used to generate deps coordintes for the project.
      "}
  fr.jeremyschoffen.mbt.alpha.default.deps
  (:require
    [clojure.spec.alpha :as s]
    [fr.jeremyschoffen.mbt.alpha.core :as mbtcore]
    [fr.jeremyschoffen.mbt.alpha.default.versioning.git-state :as git-state]
    [fr.jeremyschoffen.mbt.alpha.utils :as u]))


(u/pseudo-nss
  git
  git.commit
  git.tag
  maven
  project
  versioning)

(defn make-maven-deps-coords
  "Make the map representation of a maven dependency in a `deps.edn` file.

  For instance:
  ```clojure
  (make-deps-coord
    {:...mbt.alpha.maven/group-id 'org.clojure
     :...mbt.alpha.maven/artefact-name 'clojure
     :...mbt.alpha.project/version \"10.0.1\"})
  ;=> {:org.clojure/clojure {:mvn/version \"10.0.1\"}}
  ```
  "
  [{v   ::project/version
    :as param}]
  {(mbtcore/deps-symbolic-name param) {:mvn/version v}})

(u/spec-op make-maven-deps-coords
           :deps [mbtcore/deps-symbolic-name]
           :param {:req [::maven/group-id
                         ::maven/artefact-name
                         ::project/version]
                   :opt [::maven/classifier]}
           :ret ::project/maven-coords)


(defn make-git-deps-coords
  "Make the map representation of a git dependency in a `deps.edn` file. Here the git tag
  corresponding to the version is used to recover the commit's sha associated with it.

  For instance:
  ```clojure
  (make-deps-coord
    {:...mbt.alpha.maven/group-id 'org.something
     :...mbt.alpha.maven/artefact-name 'my-lib
     :...mbt.alpha.project/git-url \"https://github.com/yourname/my-lib\"
     :...mbt.alpha.versioning/tag-base-name \"my-lib\"
     :...mbt.alpha.versioning/version a-version})
  ;=> {:org.something/mylib {:git/url \"https://github.com/yourname/my-lib\\\"
                             :sha \"sha of the tag at my-lib-va-version\"}}
  ```
  "
  [{u ::project/git-url
    :as param}]
  (let [commit-name (-> param
                        git-state/get-tag
                        ::git.commit/name)]
    {(mbtcore/deps-symbolic-name param) {:git/url u :sha commit-name}}))

(u/spec-op make-git-deps-coords
           :deps [git-state/get-tag
                  mbtcore/deps-symbolic-name]
           :param {:req [::git/repo
                         ::maven/artefact-name
                         ::maven/group-id
                         ::project/git-url
                         ::versioning/tag-base-name
                         ::versioning/version]
                   :opt [::maven/classifier]}
           :ret ::project/git-coords)
