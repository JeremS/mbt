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
  git.commit
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
               ::project/git-url   "https://github.com/JeremS/mbt"

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


(defn bump-project! []
  (-> conf
      (u/do-side-effect! mbt-build/bump-project!)))


(defn generate-docs! [conf]
  (-> conf
      (u/assoc-computed ::versioning/version mbt-defaults/versioning-last-version
                        ::project/version mbt-defaults/versioning-project-version
                        ::project/maven-coords mbt-defaults/deps-make-maven-coords
                        ::project/git-coords mbt-defaults/deps-make-git-coords)
      (assoc-in [::git/commit! ::git.commit/message] "Generated the docs.")
      (mbt-defaults/generate-then-commit!
        (u/do-side-effect! docs/make-readme!)
        (u/do-side-effect! docs/make-rationale!)
        (u/do-side-effect! docs/make-design-doc!)
        (u/do-side-effect! docs/make-config-doc!))))


(u/spec-op generate-docs!
           :deps [mbt-defaults/versioning-last-version
                  mbt-defaults/versioning-project-version
                  mbt-defaults/deps-make-maven-coords
                  mbt-defaults/deps-make-git-coords]
           :param {:req [::git/repo
                         ::maven/artefact-name
                         ::maven/group-id
                         ::project/git-url
                         ::versioning/scheme
                         ::versioning/tag-base-name]
                   :opt [::maven/classifier
                         ::versioning/tag-base-name
                         ::versioning/version]})
(u/param-suggestions generate-docs!)



(defn build! []
  (-> conf
      (u/assoc-computed ::versioning/version mbt-defaults/versioning-last-version
                        ::project/version mbt-defaults/versioning-project-version)
      mbt-build/build!))


(st/instrument `[generate-docs!
                 mbt-defaults/generate-then-commit!
                 mbt-defaults/deps-make-maven-coords
                 mbt-defaults/deps-make-git-coords
                 mbt-build/build!
                 mbt-defaults/maven-install!
                 mbt-defaults/maven-deploy!
                 mbt-defaults/versioning-last-version])


(comment


  (mbt-core/clean! conf)

  (erase-local!))
