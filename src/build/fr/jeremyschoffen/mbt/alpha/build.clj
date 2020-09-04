(ns fr.jeremyschoffen.mbt.alpha.build
  (:require
    [clojure.spec.test.alpha :as st]
    [fr.jeremyschoffen.mbt.alpha.core :as mbt-core]
    [fr.jeremyschoffen.mbt.alpha.default :as mbt-defaults]
    [fr.jeremyschoffen.mbt.alpha.utils :as u]
    [fr.jeremyschoffen.mbt.alpha.docs.core :as docs]
    [build :refer [add-deploy-conf]]))


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


(def conf (-> {::maven/group-id    'fr.jeremyschoffen
               ::project/author    "Jeremy Schoffen"

               ::version-file/ns   'fr.jeremyschoffen.mbt.alpha.version
               ::version-file/path (u/safer-path "src" "main" "fr" "jeremyschoffen" "mbt" "alpha" "version.clj")
               ::versioning/scheme mbt-defaults/git-distance-scheme
               ::versioning/major  :alpha

               ::maven.server mbt-defaults/clojars
               ::project/licenses  [{::project.license/name "Eclipse Public License - v 2.0"
                                     ::project.license/url "https://www.eclipse.org/legal/epl-v20.html"
                                     ::project.license/distribution :repo
                                     ::project.license/file (u/safer-path "LICENSE")}]}
              mbt-defaults/config
              add-deploy-conf))


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


(defn prebuild-generation!
  "Build the docs and version file then commit."
  [conf]
  (-> conf
      (mbt-defaults/build-before-bump! (u/do-side-effect! generate-docs!)
                                       (u/do-side-effect! mbt-defaults/write-version-file!))))

(u/spec-op prebuild-generation!
           :deps [generate-docs!
                  mbt-defaults/write-version-file!]
           :param {:req [::project/version
                         ::version-file/ns
                         ::version-file/path]})


(defn new-milestone!
  "Generate docs then tag a new version."
  [conf]
  (-> conf
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


(defn next-version+1 [conf]
  (let [next-v (mbt-defaults/versioning-next-version conf)]
    (if (= next-v (mbt-defaults/versioning-initial-version conf))
      next-v
      (update next-v :number inc))))


(defn release! []
  (-> conf
      (u/assoc-computed ::versioning/version next-version+1
                        ::project/version mbt-defaults/versioning-project-version)
      (u/do-side-effect! new-milestone!)
      (u/do-side-effect! mbt-defaults/build-jar!)
      (u/do-side-effect! mbt-defaults/maven-install!)
      u/record-build))


(defn erase-local! [v]
  (-> conf
      (assoc ::project/version v)
      (u/do-side-effect! mbt-defaults/build-jar!)
      (u/do-side-effect! mbt-defaults/maven-install!)
      u/record-build))


(st/instrument `[generate-docs!
                 prebuild-generation!
                 new-milestone!
                 mbt-core/deps-make-coord
                 mbt-defaults/build-jar!
                 mbt-defaults/maven-install!
                 mbt-defaults/maven-deploy!])


(comment
  (erase-local! "0")

  (mbt-core/clean! conf)

  (erase-local!))
