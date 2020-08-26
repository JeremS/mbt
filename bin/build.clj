(ns build
  (:require
    [clojure.spec.test.alpha :as spec-test]
    [fr.jeremyschoffen.mbt.alpha.core :as mbt-core]
    [fr.jeremyschoffen.mbt.alpha.default :as mbt-defaults]
    [fr.jeremyschoffen.mbt.alpha.utils :as u]
    [docs.core :as docs]))

(u/pseudo-nss
  maven
  project
  project.license
  version-file
  versioning)

(spec-test/instrument
  `[mbt-core/deps-make-coord

    mbt-defaults/write-version-file!
    mbt-defaults/generate-before-bump!
    mbt-defaults/bump-tag!
    mbt-defaults/build-jar!
    mbt-defaults/install!])


(def specific-conf
  (sorted-map
    ::maven/group-id 'fr.jeremyschoffen
    ::project/author "Jeremy Schoffen"

    ::version-file/ns 'fr.jeremyschoffen.mbt.alpha.version
    ::version-file/path (u/safer-path "src" "main" "fr" "jeremyschoffen" "mbt" "alpha" "version.clj")
    ::versioning/scheme mbt-defaults/git-distance-scheme
    ::versioning/major :alpha

    ::project/licenses [{::project.license/name "Eclipse Public License - v 2.0"
                         ::project.license/url "https://www.eclipse.org/legal/epl-v20.html"
                         ::project.license/distribution :repo
                         ::project.license/file (u/safer-path "LICENSE")}]))


(def conf (mbt-defaults/make-conf specific-conf))


(defn generate-docs! [conf]
  (-> conf
      (u/assoc-computed ::project/maven-coords mbt-core/deps-make-coord)
      (u/do-side-effect! docs/make-readme!)
      (u/do-side-effect! docs/make-rationale!)
      (u/do-side-effect! docs/make-design-doc!)))


(defn new-milestone! [param]
  (-> param
      (mbt-defaults/generate-before-bump! (u/do-side-effect! generate-docs!)
                                          (u/do-side-effect! mbt-defaults/write-version-file!))
      (u/side-effect! mbt-defaults/bump-tag!)))


(defn deploy! [conf]
  (-> conf
      (assoc ::maven/server mbt-defaults/clojars)
      mbt-defaults/deploy!))

(comment
  (-> conf
      u/mark-dry-run
      (u/assoc-computed ::project/version (comp str mbt-defaults/anticipated-next-version)
                        ::project/maven-coords mbt-core/deps-make-coord)
      (u/do-side-effect! docs/make-readme!)
      (u/do-side-effect! docs/make-rationale!)
      (u/do-side-effect! docs/make-design-doc!))

  (new-milestone! conf)

  (mbt-core/clean! conf)

  (str (mbt-defaults/anticipated-next-version conf))

  (mbt-defaults/build-jar! conf)
  (mbt-defaults/install! conf)

  (-> conf
      (assoc ::project/version "0")
      (u/do-side-effect! mbt-defaults/build-jar!)
      (u/do-side-effect! mbt-defaults/install!)
      u/record-build))

