(ns com.jeremyschoffen.mbt.alpha.core.building.compilation
  (:require
    [clojure.tools.namespace.find :as ns-find]
    [com.jeremyschoffen.java.nio.file :as fs]

    [com.jeremyschoffen.mbt.alpha.core.specs]
    [com.jeremyschoffen.mbt.alpha.core.utils :as u]))


;; inspired by https://github.com/luchiniatwork/cambada/blob/master/src/cambada/compile.clj
(defn- dirs->nss [dirs]
  (into []
        (comp
          (map fs/file)
          (mapcat ns-find/find-namespaces-in-dir))
        dirs))

(defn project-nss
  "Find all namespace from a classpath located inside the working directory."
  [{cp :classpath/index}]
  (-> cp :classpath/dir dirs->nss))

(u/spec-op project-nss
           :param {:req [:classpath/index]}
           :ret :clojure.compilation/namespaces)


(defn external-nss
  "Find all namespace from a classpath located outside the working directory."
  [{cp :classpath/index}]
  (-> cp :ext-dep dirs->nss))

(u/spec-op external-nss
           :param {:req [:classpath/index]}
           :ret :clojure.compilation/namespaces)


;; TODO: would be nice to have something like boot's pods to isolate
;; the current environment while compiling.
(defn compile! [{output-dir :clojure.compilation/output-dir
                 namespaces :clojure.compilation/namespaces}]
  (fs/create-directories! output-dir)
  (binding [*compile-path* output-dir]
    (doseq [n namespaces]
      (compile n))))


(u/spec-op compile!
           :param {:req [:clojure.compilation/output-dir
                         :clojure.compilation/namespaces]})




