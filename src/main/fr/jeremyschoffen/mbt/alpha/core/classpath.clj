(ns ^{:author "Jeremy Schoffen"
      :doc "
Api providing utilities when manipulating classpaths generated using `clojure.tools.deps`.
      "}
  fr.jeremyschoffen.mbt.alpha.core.classpath
  (:require
    [clojure.string :as string]
    [clojure.tools.deps.alpha :as deps]
    [clojure.tools.deps.alpha.util.maven :as maven]
    [fr.jeremyschoffen.java.nio.alpha.file :as fs]
    [fr.jeremyschoffen.mbt.alpha.core.specs]
    [fr.jeremyschoffen.mbt.alpha.utils :as u]))


(u/pseudo-nss
  classpath
  project
  project.deps)

;; TODO: deps/make-classpath-map may be subject to change in the future, keep up!
(defn deps-classpath-map
  "Facade to the [[clojure.tools.deps.alpha/make-classpath-map]] function."
  [{project-deps ::project/deps
    aliases      ::project.deps/aliases}]
  (let [deps-map (update project-deps :mvn/repos #(merge maven/standard-repos %))
        args-map (deps/combine-aliases deps-map aliases)
        resolved-deps (deps/resolve-deps deps-map args-map)]
    (deps/make-classpath-map (select-keys deps-map #{:paths})
                             resolved-deps
                             args-map)))

(u/spec-op deps-classpath-map
           :param {:req [::project/deps]
                   :opt [::project.deps/aliases]})


(defn- sanitize-classpath [paths wd]
  (into [] (comp
             (map (partial fs/resolve wd))
             (map str))
    paths))


(defn- jar? [path]
  (string/ends-with? path ".jar"))


(defn- project-path? [wd path]
  (fs/ancestor? wd path))


(defn- classify
  "Classifies the different entries of a classpath. Categories are:
  - :fr...mbt.alpha.classpath/jar: path to a jar
  - :fr...mbt.alpha.classpath/dir: path to a directory that is in the working dir
  - :fr...mbt.alpha.classpath/ext-dep: path to a directory outside the working dir
  - :fr...mbt.alpha.classpath/nonexisting: path in the classpath that leads to nothing
  - :fr...mbt.alpha.classpath/file: individual file on the classpath
  "
  [wd path]
  (cond
    (not (fs/exists? path)) ::classpath/nonexisting
    (jar? path) ::classpath/jar
    (fs/directory? path) (if (project-path? wd path)
                           ::classpath/dir
                           ::classpath/ext-dep)
    :else ::classpath/file))


(defn- index-classpath [paths wd]
  (-> paths
      (sanitize-classpath wd)
      (->> (group-by (partial classify wd)))))


(defn indexed-classpath
  "Construct a classpath map using [[clojure.tools.deps.alpha/make-classpath-map]] takes the keys and group them
  into a map. The keys of this map are defined in [[fr.jeremyschoffen.mbt.alpha.core.specs/classpath-index-categories]],
  the values are a seq of classpath entries corresponding to the categories.
  "
  [{wd ::project/working-dir
    :as param}]
  (-> param
      deps-classpath-map
      keys
      (index-classpath wd)))

(u/spec-op indexed-classpath
           :deps [deps-classpath-map]
           :param {:req [::project/working-dir
                         ::project/deps]
                   :opt [::project.deps/aliases]}
           :ret ::classpath/index)

(def path-separator (System/getProperty "path.separator"))

(defn raw-classpath
  "Returns the a string representing the classpath given the config in
  `param`. The paths inside it are all absolute."
  [{wd ::project/working-dir
    :as param}]
  (-> param
      deps-classpath-map
      keys
      (sanitize-classpath wd)
      (->> (clojure.string/join path-separator))))

(u/spec-op raw-classpath
           :deps [deps-classpath-map]
           :param {:req [::project/deps]
                   :opt [::project.deps/aliases]}
           :ret ::classpath/raw)
