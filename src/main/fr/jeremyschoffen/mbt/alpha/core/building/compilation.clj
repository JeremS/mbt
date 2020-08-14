(ns ^{:author "Jeremy Schoffen"
      :doc "
Api providing clojure compilation utilities.
      "}
  fr.jeremyschoffen.mbt.alpha.core.building.compilation
  (:require
    [clojure.tools.namespace.find :as ns-find]
    [fr.jeremyschoffen.java.nio.alpha.file :as fs]

    [fr.jeremyschoffen.mbt.alpha.core.specs]
    [fr.jeremyschoffen.mbt.alpha.utils :as u])
  (:import
    [java.util.jar JarFile]))

;; TODO: provide a story for java files.
;; inspired by https://github.com/luchiniatwork/cambada/blob/master/src/cambada/compile.clj
(defn- dirs->nss [dirs]
  (into []
        (comp
          (map fs/file)
          (mapcat ns-find/find-namespaces-in-dir))
        dirs))

(defn project-nss
  "Use an indexed classpath to find all Clojure namespaces from src directories located inside the working directory.

  See:
    - [[fr.jeremyschoffen.mbt.alpha.core.building.classpath/indexed-classpath]]
  "
  [{cp :classpath/index}]
  (-> cp :classpath/dir dirs->nss))

(u/spec-op project-nss
           :param {:req [:classpath/index]}
           :ret :clojure.compilation/namespaces)


(defn external-nss
  "Use an indexed classpath to find all Clojure namespaces from src directories located outside the working directory.
  These would be namespaces from local deps or directly from a git repo.

  See:
    - [[fr.jeremyschoffen.mbt.alpha.core.building.classpath/indexed-classpath]]
  "
  [{cp :classpath/index}]
  (-> cp :ext-dep dirs->nss))

(u/spec-op external-nss
           :param {:req [:classpath/index]}
           :ret :clojure.compilation/namespaces)


(defn jar-nss
  "Use an indexed classpath to find all Clojure namespaces from jars.

  See:
    - [[fr.jeremyschoffen.mbt.alpha.core.building.classpath/indexed-classpath]]
  "
  [{cp :classpath/index}]
  (into []
        (comp
          (map #(JarFile. ^String %))
          (mapcat ns-find/find-namespaces-in-jarfile))
        (:classpath/jar cp)))

(u/spec-op jar-nss
           :param {:req [:classpath/index]}
           :ret :clojure.compilation/namespaces)

;; TODO: would be nice to have something like boot's pods to isolate
;; the current environment while compiling.
(defn compile!
  "Compile a list of namespaces provided under the key `:clojure.compilation/namespaces`, the results are placed at the
  location specified under the key `:clojure.compilation/output-dir`."
  [{output-dir :clojure.compilation/output-dir
    namespaces :clojure.compilation/namespaces}]
  (fs/create-directories! output-dir)
  (binding [*compile-path* output-dir]
    (doseq [n namespaces]
      (compile n))))

(u/spec-op compile!
           :param {:req [:clojure.compilation/output-dir
                         :clojure.compilation/namespaces]})

(comment
  (require '[fr.jeremyschoffen.mbt.alpha.core.building.deps :as deps])
  (require '[fr.jeremyschoffen.mbt.alpha.core.building.classpath :as cp])

  (-> {:project/working-dir (u/safer-path)}
      (u/assoc-computed :project/deps deps/get-deps
                        :classpath/index cp/indexed-classpath)
      jar-nss))