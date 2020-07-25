# Mbt

A build tools designed to be used with [tools.deps](https://github.com/clojure/tools.deps.alpha).

## Usage
Mbt provides apis to help create build scripts written in clojure. The api works as follow:
- each build task is a function of one argument
- this one argument is a map containing the configuration
- using some utilities we can chain several tasks

Documentation is to be found here. 

To get started however, this project's own build script might help describe mbt's usage.

We have a standard ns declaration pulling in the core api, the default api and some utilities.
```clojure
(ns build
  (:require
    [clojure.spec.test.alpha :as spec-test]
    [com.jeremyschoffen.mbt.alpha.core :as mbt-core]
    [com.jeremyschoffen.mbt.alpha.default :as mbt-defaults]
    [com.jeremyschoffen.mbt.alpha.utils :as u]))
```

We can instrument our build tasks. 
```clojure
(spec-test/instrument
  [mbt-defaults/add-version-file!
   mbt-defaults/bump-tag!
   mbt-defaults/build-jar!
   mbt-defaults/install!])
```

We declare some configuration specific to this project:
```clojure
(def specific-conf
  {:versioning/scheme mbt-defaults/simple-scheme
   :versioning/major :alpha
   :project/author "Jeremy Schoffen"
   :version-file/ns 'com.jeremyschoffen.mbt.alpha.version
   :version-file/path (u/safer-path "src" "com" "jeremyschoffen" "mbt" "alpha" "version.clj")})
```

We fill the rest of the configuration with default values. 
```clojure
(def conf (->> specific-conf
               mbt-defaults/make-conf
               (into (sorted-map))))
```

We can define a specific step that writes a version file then creates a new git tag fixing a new version of the project.
```clojure
(defn new-milestone! [param]
  (-> param
      (u/side-effect! mbt-defaults/add-version-file!)
      (u/side-effect! mbt-defaults/bump-tag!)))
```

Since I use this ns at the repl, I have just put some build ing tasks in a comment
```clojure
(comment
  (new-milestone! conf)

  (mbt-core/clean! conf)

  (mbt-defaults/build-jar! conf)
  (mbt-defaults/install! conf))
``` 
## Limitations
This project is very new. Not everything you might want of such a tools is present yet.

Some aspects still need some work:
- There is help with javac provided yet. 
- The clojure compilation story is very limited. It's just a thin wrapper around `clojure.core/compile`. 
I'd like to add something more developped, like compilation in a separated classloader for instance.  
- Building jars from exclusively aot compiled sources is absolutely possible but there is nothing to help you do that 
yet.
- Right now the git tag gereated by the version scheme can't be signed.

## Inspirations
This project is heavily inspired by other build tools. The ability to study these code bases helped a lot.

- [Boot](https://boot-clj.com/)
- [Badigeon](https://github.com/EwenG/badigeon)
- [Cambada](https://github.com/luchiniatwork/cambada)
- [Depstar](https://github.com/seancorfield/depstar)
- [Metav](https://github.com/jgrodziski/metav)
- [Leiningen](https://leiningen.org/)



## License

Copyright © 2019-2020 Jeremy Schoffen.

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
