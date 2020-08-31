(ns fr.jeremyschoffen.mbt.alpha.build
  (:require
    [clojure.spec.test.alpha :as spec-test]
    [fr.jeremyschoffen.mbt.alpha.core :as mbt-core]
    [fr.jeremyschoffen.mbt.alpha.default :as mbt-defaults]
    [fr.jeremyschoffen.mbt.alpha.utils :as u]
    [docs.core :as docs]))

(u/pseudo-nss
  maven
  maven.pom
  project
  project.license
  version-file
  versioning)

(spec-test/instrument
  `[mbt-core/deps-make-coord
    mbt-defaults/write-version-file!
    mbt-defaults/build-before-bump!
    mbt-defaults/versioning-tag-new-version!
    mbt-defaults/build-jar!
    mbt-defaults/maven-install!])


(def conf
  (mbt-defaults/config
    {::maven/group-id    'fr.jeremyschoffen
     ::project/author    "Jeremy Schoffen"

     ::version-file/ns   'fr.jeremyschoffen.mbt.alpha.version
     ::version-file/path (u/safer-path "src" "main" "fr" "jeremyschoffen" "mbt" "alpha" "version.clj")
     ::versioning/scheme mbt-defaults/git-distance-scheme
     ::versioning/major  :alpha

     ::project/licenses  [{::project.license/name "Eclipse Public License - v 2.0"
                           ::project.license/url "https://www.eclipse.org/legal/epl-v20.html"
                           ::project.license/distribution :repo
                           ::project.license/file (u/safer-path "LICENSE")}]}))


(defn generate-docs! [conf]
  (-> conf
      (u/assoc-computed ::project/maven-coords mbt-core/deps-make-coord)
      (u/do-side-effect! docs/make-readme!)
      (u/do-side-effect! docs/make-rationale!)
      (u/do-side-effect! docs/make-design-doc!)
      (u/do-side-effect! docs/make-config-doc!)))


(def next-version (mbt-defaults/versioning-make-next-version+x 1))

(defn new-milestone! [param]
  (-> param
      (u/assoc-computed ::versioning/version next-version
                        ::project/version (comp str ::versioning/version))
      (mbt-defaults/build-before-bump! (u/do-side-effect! generate-docs!)
                                       (u/do-side-effect! mbt-defaults/write-version-file!))
      (u/do-side-effect! mbt-defaults/versioning-tag-new-version!)
      (u/do-side-effect! mbt-defaults/build-jar!)
      (u/do-side-effect! mbt-defaults/maven-install!)))


(defn deploy! [conf]
  (-> conf
      (assoc ::maven/server mbt-defaults/clojars)
      mbt-defaults/maven-deploy!))


(comment
  ;(require '[clj-async-profiler.core :as async-p])
  ;(async-p/profile)
  (-> conf
      ;u/mark-dry-run
      (u/assoc-computed ::versioning/version next-version
                        ::project/version (comp str ::versioning/version)
                        ::project/maven-coords mbt-core/deps-make-coord)
      (->> (into (sorted-map)))
      (u/do-side-effect! docs/make-readme!)
      (u/do-side-effect! docs/make-rationale!)
      (u/do-side-effect! docs/make-design-doc!)
      (u/do-side-effect! docs/make-config-doc!))

  (new-milestone! conf)

  (mbt-core/clean! conf)

  (str (mbt-defaults/anticipated-next-version conf))

  (mbt-defaults/build-jar! conf)
  (mbt-defaults/maven-install! conf)

  (mbt-defaults/current-project-version conf)
  (mbt-defaults/next-project-version conf)
  (-> conf
      (assoc ::project/version "0")
      (u/do-side-effect! mbt-defaults/build-jar!)
      (u/do-side-effect! mbt-defaults/maven-install!)
      u/record-build))
