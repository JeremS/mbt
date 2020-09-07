(ns fr.jeremyschoffen.mbt.alpha.mbt-style
  (:require
    [clojure.spec.alpha :as s]
    [cognitect.anomalies :as anom]
    [fr.jeremyschoffen.mbt.alpha.default :as mbt-defaults]
    [fr.jeremyschoffen.mbt.alpha.utils :as u]))


(u/pseudo-nss
  build
  build.jar
  git
  git.commit
  jar
  jar.manifest
  maven
  maven.pom
  maven.scm
  project
  project.deps
  versioning
  version-file)


(defn next-version+1
  "Compute the next project version anticipating the commit adding the version file.
  Using git-distance is expected here."
  [{scheme ::versioning/scheme :as conf}]
  (when-not (= scheme mbt-defaults/git-distance-scheme)
    (throw (ex-info "Can't use this versioning scheme."
                    {::anom/category ::anom/incorrect
                     :scheme scheme})))

  (let [next-v (mbt-defaults/versioning-next-version conf)]
    (if (= next-v (mbt-defaults/versioning-initial-version conf))
      next-v
      (update next-v :number inc))))

(u/spec-op next-version+1
           :deps [mbt-defaults/versioning-next-version
                  mbt-defaults/versioning-initial-version]
           :param {:req [::git/repo
                         ::versioning/scheme
                         ::versioning/tag-base-name]
                   :opt [::versioning/bump-level]})


(defn bump-project!
  "Generate new version, docs version file... The doc generation is passed as a function under the key
   `:...mbt.alpha.build/prebuild-generation`.

  The repo is then tagged with the new version."
  [conf]
  (-> conf
      (u/assoc-computed ::versioning/version next-version+1
                        ::project/version mbt-defaults/versioning-project-version)
      (assoc-in [::git/commit! ::git.commit/message] "Bump project - Added version file.")
      (mbt-defaults/generate-then-commit! (u/do-side-effect! mbt-defaults/write-version-file!))
      (u/do-side-effect! mbt-defaults/versioning-tag-new-version!)))

(u/spec-op bump-project!
           :deps [next-version+1
                  mbt-defaults/write-version-file!
                  mbt-defaults/versioning-tag-new-version!]
           :param {:req [::git/repo
                         ::project/working-dir
                         ::version-file/ns
                         ::version-file/path
                         ::versioning/scheme
                         ::versioning/tag-base-name]
                   :opt [::versioning/bump-level]})


(defn update-scm-tag [conf]
  (let [commit (-> conf
                   mbt-defaults/versioning-get-tag
                   ::git.commit/name)]
    (update conf ::maven/scm #(assoc % ::maven.scm/tag commit))))

(u/spec-op update-scm-tag
           :deps [mbt-defaults/versioning-get-tag]
           :param {:req [::git/repo
                         ::versioning/tag-base-name
                         ::versioning/version]})

(defn build!
  [conf]
  (-> conf
      (u/assoc-computed ::versioning/version mbt-defaults/versioning-last-version
                        ::project/version mbt-defaults/versioning-project-version)
      update-scm-tag
      (u/do-side-effect! mbt-defaults/maven-sync-pom!)
      (u/do-side-effect! mbt-defaults/build-jar!)))

(u/spec-op build!
           :deps [mbt-defaults/maven-sync-pom!
                  mbt-defaults/build-jar!]
           :param {:req [::build.jar/allow-non-maven-deps
                         ::build.jar/path
                         ::git/repo
                         ::maven/artefact-name
                         ::maven/group-id
                         ::maven/scm
                         ::maven.pom/path
                         ::project/deps
                         ::project/version
                         ::project/working-dir]
                   :opt [::jar/exclude?
                         ::jar/main-ns
                         ::jar.manifest/overrides
                         ::project/author
                         ::project/licenses
                         ::project.deps/aliases]})

