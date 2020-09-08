


# Mbt

A collection of build utilities designed to be used with [tools.deps](https://github.com/clojure/tools.deps.alpha).


## Installation
Deps coords:
```clojure
{fr.jeremyschoffen/mbt-alpha {:mvn/version "0"}}
```
Lein coords:
```clojure
[fr.jeremyschoffen/mbt-alpha "0"]
```
Git coords:
```clojure
{fr.jeremyschoffen/mbt-alpha {:git/url "https://github.com/JeremS/mbt", :sha "f74159a2ff3d24d660024f41d6941245fbb8da27"}}
```

## Usage
Mbt is an experiment that aims to provide APIs to build, package and deploy clojure projects. In addition it provides
tools to version a project using git tags. The main motivation behind the project is to be able to build and deploy
artefacts at the repl. As much as this motivation is fulfilled, writing build scripts to launch using [tools.deps](https://github.com/clojure/tools.deps.alpha)
should be easy.

Mbt's own build can give an idea:
```clojure
(ns fr.jeremyschoffen.mbt.alpha.build
  (:require
    [clojure.spec.test.alpha :as st]
    [fr.jeremyschoffen.mbt.alpha.core :as mbt-core]
    [fr.jeremyschoffen.mbt.alpha.default :as mbt-defaults]
    [fr.jeremyschoffen.mbt.alpha.mbt-style :as mbt-build]
    [fr.jeremyschoffen.mbt.alpha.utils :as u]
    [fr.jeremyschoffen.mbt.alpha.docs.core :as docs]
    [build :refer [token]]))

;; Some aliases to shorten keywords
(u/pseudo-nss
  git
  git.commit
  maven
  maven.credentials
  project
  project.license
  version-file
  versioning)

;; Definition of the projects's configuration
(def conf (mbt-defaults/config
            {::maven/group-id    'fr.jeremyschoffen
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
                                   ::project.license/file (u/safer-path "LICENSE")}]}))

(defn generate-docs! [conf]
  (-&gt; conf
      mbt-build/merge-last-version
      (u/assoc-computed ::project/maven-coords mbt-defaults/deps-make-maven-coords
                        ::project/git-coords mbt-defaults/deps-make-git-coords)
      (assoc-in [::git/commit! ::git.commit/message] "Generated the docs.")
      (mbt-defaults/generate-then-commit!
        (u/do-side-effect! docs/make-readme!)
        (u/do-side-effect! docs/make-rationale!)
        (u/do-side-effect! docs/make-design-doc!)
        (u/do-side-effect! docs/make-config-doc!))))


;; generate a version file and tags the repo with a new version
(defn bump-project! []
  (-&gt; conf
      (u/do-side-effect! mbt-build/bump-project-with-version-file!)
      (u/do-side-effect! generate-docs!)))

;; instrumentation of our commands
(st/instrument `[generate-docs!
                 mbt-defaults/generate-then-commit!
                 mbt-defaults/deps-make-maven-coords
                 mbt-defaults/deps-make-git-coords
                 mbt-build/merge-last-version
                 mbt-build/build!
                 mbt-build/install!
                 mbt-build/deploy!])


;; some commands to use at the repl
(comment
  (mbt-core/clean! conf)

  (bump-project!)

  (mbt-build/build! conf)

  (mbt-build/install! conf)

  (mbt-build/deploy! conf))
```

The project is designed as follow:
- Each API function takes only one argument and returns whatever is relevant
- This one argument is a map understood to be the config
- We can chain build operations using the `-&gt;` macro and some utilities.

Three namespaces gather the functionality:
- `...mbt.alpha.core`:  the core API which is a build tools box?
- `...mbt.alpha.default`: API  siting on top of the first one, trying to tie together the different parts of the
core API.
- `...mbt.alpha.utils`: utils used throughout the APIs providing useful building blocks to make build scripts.


## Limitations
This project is very new. Not everything you might want of such a tool is present. At this stage it
is more of a test bed for several ideas and design explorations.

However it's useful enough for what I want to do. It has tests, it builds and deploys itself in addition to other
projects of mine.

Still some aspects need some work:
- The clojure compilation story is very limited. It's just a thin wrapper around `clojure.core/compile`.
I'd like to add something more developed, like compilation in a separated classloader for instance. The dream would be
boot pods.
- There is no default way of building jars of aot compiled classes. It can be made by hand using the core api.
- Right now the git tags generated by the versionnig schemes can't be signed.
- The maven deploy story doesn't support the whole range of options yet.
- Error reporting and build reporting isn't fully developed yet.



## Inspirations
This project is heavily inspired by other build tools in particular and projects in general.
The ability to study these code bases helps a lot.

- [Boot](https://boot-clj.com/)
- [Badigeon](https://github.com/EwenG/badigeon)
- [Cambada](https://github.com/luchiniatwork/cambada)
- [Depstar](https://github.com/seancorfield/depstar)
- [Leiningen](https://leiningen.org/)
- [Metav](https://github.com/jgrodziski/metav)
- [Meander](https://github.com/noprompt/meander)
- [Ring](https://github.com/ring-clojure/ring)
- [tools.deps](https://github.com/clojure/tools.deps.alpha)


## License

Copyright © 2019-2020 Jeremy Schoffen.

Distributed under the Eclipse Public License v 2.0.
