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
  gpg
  jar
  jar.manifest
  maven
  maven.deploy
  maven.install
  maven.pom
  maven.scm
  maven.settings
  project
  project.deps
  versioning
  version-file)

(defn bump-project!
  "Generate a new version and tag the repo."
  [conf]
  (-> conf
      (u/assoc-computed ::versioning/version mbt-defaults/versioning-next-version
                        ::project/version mbt-defaults/versioning-project-version)
      (u/do-side-effect! mbt-defaults/versioning-tag-new-version!)))

(u/spec-op bump-project!
           :deps [mbt-defaults/versioning-next-version
                  mbt-defaults/versioning-tag-new-version!]
           :param {:req [::git/repo
                         ::project/working-dir
                         ::versioning/scheme
                         ::versioning/tag-base-name]
                   :opt [::versioning/bump-level]})

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


(defn bump-project-with-version-file!
  "Generate a new version file then tags a new version. The additionnal commit for the version file
  is taken into account when computing the new version."
  [conf]
  (-> conf
      (u/assoc-computed ::versioning/version next-version+1
                        ::project/version mbt-defaults/versioning-project-version)
      (assoc-in [::git/commit! ::git.commit/message] "Bump project - Added version file.")
      (mbt-defaults/generate-then-commit! (u/do-side-effect! mbt-defaults/write-version-file!))
      (u/do-side-effect! mbt-defaults/versioning-tag-new-version!)))

(u/spec-op bump-project-with-version-file!
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


(defn merge-last-version
  "Add to a config map the keys `::versioning/version` and `::project/version`"
  [conf]
  (-> conf
      (u/assoc-computed ::versioning/version mbt-defaults/versioning-last-version
                        ::project/version mbt-defaults/versioning-project-version)))

(u/spec-op merge-last-version
           :deps [mbt-defaults/versioning-last-version
                  mbt-defaults/versioning-project-version]
           :param {:req [::git/repo
                         ::versioning/scheme
                         ::versioning/tag-base-name]}
           :ret (s/keys :req [::versioning/version
                              ::project/version]))


(defn build!
  [conf]
  (-> conf
      merge-last-version
      mbt-defaults/versioning-update-scm-tag
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
                         ::project/working-dir]
                   :opt [::jar/exclude?
                         ::jar/main-ns
                         ::jar.manifest/overrides
                         ::project/author
                         ::project/licenses
                         ::project.deps/aliases]})


(defn install! [conf]
  (-> conf
      merge-last-version
      mbt-defaults/maven-install!))

(u/spec-op install!
           :deps [merge-last-version
                  mbt-defaults/maven-install!]
           :param {:req [::build.jar/path
                         ::git/repo
                         ::maven/artefact-name
                         ::maven/group-id
                         ::maven.pom/path
                         ::versioning/scheme
                         ::versioning/tag-base-name]
                   :opt [::maven/classifier
                         ::maven.deploy/artefacts
                         ::maven.install/dir]})


(defn deploy! [conf]
  (-> conf
      merge-last-version
      mbt-defaults/maven-deploy!))

(u/spec-op deploy!
           :deps [merge-last-version
                  mbt-defaults/maven-deploy!]
           :param {:req [::build.jar/path
                         ::git/repo
                         ::maven/artefact-name
                         ::maven/group-id
                         ::maven/server
                         ::maven.deploy/sign-artefacts?
                         ::maven.pom/path
                         ::versioning/scheme
                         ::versioning/tag-base-name]
                   :opt [::gpg/command
                         ::gpg/home-dir
                         ::gpg/key-id
                         ::gpg/pass-phrase
                         ::gpg/version
                         ::maven/classifier
                         ::maven/credentials
                         ::maven/local-repo
                         ::maven.deploy/artefacts
                         ::maven.settings/file
                         ::project/working-dir]})
