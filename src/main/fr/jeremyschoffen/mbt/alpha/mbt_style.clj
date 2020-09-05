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
  jar
  jar.manifest
  maven
  maven.pom
  project
  project.deps
  versioning
  version-file)


(s/def ::build/prebuild-generation
  (s/fspec :args (s/keys :req [::project/version]
                         :opt [::version-file/ns
                               ::version-file/path])))


(defn next-version+1
  "Compute the next project version anticipating the commit adding the docs.
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
                         ::versioning/scheme]
                   :opt [::versioning/bump-level
                         ::versioning/tag-base-name]})


(defn bump-project!
  "Generate new version, docs version file... The doc generation is passed as a function under the key
   `:...mbt.alpha.build/prebuild-generation`.

  The repo is then tagged with the new version."
  [{prebuild-generation! ::build/prebuild-generation
    :as conf}]
  (-> conf
      (u/assoc-computed ::versioning/version next-version+1
                        ::project/version mbt-defaults/versioning-project-version)
      (u/do-side-effect-named! prebuild-generation! 'prebuild-generation!)
      (u/do-side-effect! mbt-defaults/versioning-tag-new-version!)))

(u/spec-op bump-project!
           :deps [next-version+1
                  mbt-defaults/versioning-tag-new-version!]
           :param {:req [::build/prebuild-generation
                         ::git/repo
                         ::project/working-dir
                         ::versioning/scheme
                         ::versioning/tag-base-name
                         ::versioning/version]
                   :opt [::version-file/ns
                         ::version-file/path]})
(u/param-suggestions bump-project!)


(defn build!
  [conf]
  (-> conf
      mbt-defaults/maven-make-github-scm
      (u/do-side-effect! mbt-defaults/maven-sync-pom!)
      (u/do-side-effect! mbt-defaults/build-jar!)))

(u/spec-op build!
           :deps [mbt-defaults/maven-make-github-scm
                  mbt-defaults/maven-sync-pom!
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

