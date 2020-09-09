(ns ^{:author "Jeremy Schoffen"
      :doc "
In mbt's default api, several config value are computed by default. This API provides the mechanisms used to construct
a project's config map.

Lets take an example to illustrate the way this API is tu be used.

The name of an artefact in tools deps is a symbol whose namespace represents its group id and whose name represents
its general name. The name is itself subject to be divided into artefact name and classifier. Mbt also has a concept of
major version that will add a suffix to artefact names.

Let's say we are building an artefact named `com.my-group/my-project-alpha$sources.`
A simplified config could then look like this:
```clojure
(def conf {:project/name 'my-project
           :maven/group-id 'com.my-group
           :maven/classifier 'sources
           :versioning/major :alpha

           :maven/artefact-name (calc compute-artefact-name :project/name :versioning/major)
           :computed-coordinate (calc compute-coord-name :maven/artefact-name
                                                         :maven/group-id
                                                         :maven/classifier)})
```
Here, the first 4 values in the map are given. The last 2 are to be computed, the second one depending on the result of
the first. These computations are 'declared' with the `(calc computation & dependencies)` scheme.
Assuming we have the functions such as:
```clojure
(compute-artefact-name {:project/name 'my-project
                        :versioning/major :alpha})
;=> 'my-project-alpha


(compute-artefact-name {:maven/artefact-name 'my-project-alpha
                        :maven/group-id 'com.my-group
                        :maven/classifier 'sources})
;=> 'com.my-group/my-project-alpha$sources
```
The compute utility perform several operations with this config. It will:

1) find the keys that need to be computed (`:maven/artefact-name` & `:computed-coordinate`)
2) establish a dependency graph between these computation
3) run the computations in order to satisfy dependencies (topological sort of the dependency graph)
4) return a map based on the original where the value that were computations are now the result of these computations.

```clojure
(compute-conf conf)
;=> {:project/name 'my-project
     :maven/group-id 'com.my-group
     :maven/classifier 'sources
     :versioning/major :alpha

     :maven/artefact-name 'my-project-alpha
     :computed-coordinate 'com.my-group/my-project-alpha$sources}
```

`:maven/artefact-name` and  `:computed-coordinate` have been computed based on other values in the config map.
      "}
  fr.jeremyschoffen.mbt.alpha.default.config
  (:require
    [ubergraph.core :as graph]
    [fr.jeremyschoffen.dolly.core :as dolly]
    [fr.jeremyschoffen.mbt.alpha.default.config.build :as build-c]
    [fr.jeremyschoffen.mbt.alpha.default.config.cleaning :as cleaning-c]
    [fr.jeremyschoffen.mbt.alpha.default.config.compilation :as compi-c]
    [fr.jeremyschoffen.mbt.alpha.default.config.git :as git-c]
    [fr.jeremyschoffen.mbt.alpha.default.config.gpg :as gpg-c]
    [fr.jeremyschoffen.mbt.alpha.default.config.impl :as impl]
    [fr.jeremyschoffen.mbt.alpha.default.config.maven :as maven-c]
    [fr.jeremyschoffen.mbt.alpha.default.config.project :as project-c]
    [fr.jeremyschoffen.mbt.alpha.default.config.versioning :as versioning-conf]))


(dolly/def-clone calc impl/calc)
(dolly/def-clone compute-conf impl/compute)

(defn clone-key
  "Make a calc using [[fr.jeremyschoffen.mbt.alpha.default.config/calc]]
  to easily duplicate a value in the config.

  ```clojure
  (calc :k :k)
  ; <=>
  (clone-val :k)
  ```"
  [k]
  (calc #(get % k) k))


(defn pprint-deps
  "Use ubergraph to pprint the dependency graph of config values."
  [config]
  (-> config
      impl/extract-calcs
      graph/digraph
      graph/pprint))


(def base
  "The base config containing default values and constructors (see [[fr.jeremyschoffen.mbt.alpha.default.config/calc]])
  for config keys."
  (merge
    project-c/conf
    cleaning-c/conf
    compi-c/conf
    git-c/conf
    gpg-c/conf2
    versioning-conf/conf
    build-c/conf
    maven-c/conf))


(defn make-base-config
  "Make the config for a project, `user-defined` values or constructors are merged into
  [[fr.jeremyschoffen.mbt.alpha.default.config/base]] then the config is computed using
  [[fr.jeremyschoffen.mbt.alpha.default.config/compute-conf]]."
  ([]
   (make-base-config {}))
  ([user-defined]
   (compute-conf (merge base user-defined))))
