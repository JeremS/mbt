(ns fr.jeremyschoffen.mbt.alpha.build
  (:require
    [fr.jeremyschoffen.mbt.alpha.core :as mbt-core]
    [fr.jeremyschoffen.mbt.alpha.default :as mbt-defaults]
    [fr.jeremyschoffen.mbt.alpha.utils :as u]
    [fr.jeremyschoffen.mbt.alpha.docs.core :as docs]))


(u/pseudo-nss
  build.jar
  git
  jar
  jar.manifest
  maven
  maven.pom
  maven.deploy
  maven.install
  project
  project.deps
  project.license
  version-file
  versioning)


(def conf {::maven/group-id    'fr.jeremyschoffen
           ::project/author    "Jeremy Schoffen"

           ::version-file/ns   'fr.jeremyschoffen.mbt.alpha.version
           ::version-file/path (u/safer-path "src" "main" "fr" "jeremyschoffen" "mbt" "alpha" "version.clj")
           ::versioning/scheme mbt-defaults/git-distance-scheme
           ::versioning/major  :alpha

           ::project/licenses  [{::project.license/name "Eclipse Public License - v 2.0"
                                 ::project.license/url "https://www.eclipse.org/legal/epl-v20.html"
                                 ::project.license/distribution :repo
                                 ::project.license/file (u/safer-path "LICENSE")}]})



(defn generate-docs! [conf]
  (-> conf
      (u/assoc-computed ::project/maven-coords mbt-core/deps-make-coord)
      (u/do-side-effect! docs/make-readme!)
      (u/do-side-effect! docs/make-rationale!)
      (u/do-side-effect! docs/make-design-doc!)
      (u/do-side-effect! docs/make-config-doc!)))


(u/spec-op generate-docs!
           :deps [mbt-core/deps-make-coord]
           :param {:req [::maven/artefact-name
                         ::maven/group-id
                         ::project/version]
                   :opt [::maven/classifier]})




(def next-version+1 (mbt-defaults/versioning-make-next-version+x 1))

(u/spec-op next-version+1
           :deps [mbt-defaults/versioning-next-version]
           :param {:req [::git/repo
                         ::versioning/scheme]
                   :opt [::versioning/bump-level
                         ::versioning/tag-base-name]})



(defn merge-version+1 [conf]
  (-> conf
    (u/assoc-computed ::versioning/version next-version+1
                      ::project/version (comp str ::versioning/version))))

(u/spec-op merge-version+1
           :deps [next-version+1]
           :param {:req [::git/repo
                         ::versioning/scheme]
                   :opt [::versioning/bump-level
                         ::versioning/tag-base-name]})


(defn prebuild-generation! [conf]
  (-> conf
      (mbt-defaults/build-before-bump! (u/do-side-effect! generate-docs!)
                                       (u/do-side-effect! mbt-defaults/write-version-file!))))

(u/spec-op prebuild-generation!
           :deps [generate-docs!
                  mbt-defaults/write-version-file!]
           :param {:req [::project/version
                         ::version-file/ns
                         ::version-file/path]})


(defn new-milestone! [conf]
  (-> conf
      merge-version+1
      (u/do-side-effect! prebuild-generation!)
      (u/do-side-effect! mbt-defaults/versioning-tag-new-version!)))

(u/spec-op new-milestone!
           :deps [prebuild-generation!
                  mbt-defaults/versioning-tag-new-version!]
           :param {:req #{::git/repo
                          ::project/version
                          ::project/working-dir
                          ::version-file/ns
                          ::version-file/path
                          ::versioning/tag-base-name
                          ::versioning/version}})


(defn jar&install! [conf]
  (-> conf
      (u/do-side-effect! mbt-defaults/build-jar!)
      (u/do-side-effect! mbt-defaults/maven-install!)))

(u/spec-op jar&install!
           :deps [mbt-defaults/build-jar!
                  mbt-defaults/maven-install!]
           :param {:req #{::build.jar/allow-non-maven-deps
                          ::build.jar/path
                          ::maven/artefact-name
                          ::maven/group-id
                          ::maven.pom/path
                          ::project/deps
                          ::project/version
                          ::project/working-dir},
                   :opt #{::jar/exclude?
                          ::jar/main-ns
                          ::jar.manifest/overrides
                          ::maven/classifier
                          ::maven/scm
                          ::maven.deploy/artefacts
                          ::maven.install/dir
                          ::project/author
                          ::project/licenses
                          ::project.deps/aliases}})
