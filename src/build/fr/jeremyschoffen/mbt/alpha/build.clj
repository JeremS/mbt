(ns fr.jeremyschoffen.mbt.alpha.build
  (:require
    [clojure.spec.test.alpha :as st]
    [fr.jeremyschoffen.mbt.alpha.core :as mbt-core]
    [fr.jeremyschoffen.mbt.alpha.default :as mbt-defaults]
    [fr.jeremyschoffen.mbt.alpha.mbt-style :as mbt-build]
    [fr.jeremyschoffen.mbt.alpha.utils :as u]
    [fr.jeremyschoffen.mbt.alpha.docs.core :as docs]
    [build :refer [token]]))


(u/pseudo-nss
  build
  build.jar
  git
  jar
  jar.manifest
  maven
  maven.credentials
  maven.deploy
  maven.install
  maven.pom
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
               ::maven/credentials {::maven.credentials/user-name "jeremys"
                                    ::maven.credentials/password token}

               ::project/licenses  [{::project.license/name "Eclipse Public License - v 2.0"
                                     ::project.license/url "https://www.eclipse.org/legal/epl-v20.html"
                                     ::project.license/distribution :repo
                                     ::project.license/file (u/safer-path "LICENSE")}]}
              mbt-defaults/config))



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


(defn bump-project! []
  (-> conf
      (assoc ::build/prebuild-generation prebuild-generation!)
      (u/do-side-effect! mbt-build/bump-project!)))


(defn build! []
  (-> conf
      (u/assoc-computed ::versioning/version mbt-defaults/versioning-last-version
                        ::project/version mbt-defaults/versioning-project-version)
      mbt-build/build!))


(st/instrument `[generate-docs!
                 prebuild-generation!
                 mbt-core/deps-make-coord
                 mbt-build/build!
                 mbt-defaults/maven-install!
                 mbt-defaults/maven-deploy!
                 mbt-defaults/versioning-last-version])


(comment


  (mbt-core/clean! conf)

  (erase-local!))
