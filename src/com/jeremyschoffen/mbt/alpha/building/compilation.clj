(ns com.jeremyschoffen.mbt.alpha.building.compilation
  (:require
    [clojure.spec.alpha :as s]
    [clojure.tools.namespace.find :as ns-find]
    [com.jeremyschoffen.java.nio.file :as fs]

    [com.jeremyschoffen.mbt.alpha.specs]
    [com.jeremyschoffen.mbt.alpha.utils :as u]))


;; inspired by https://github.com/luchiniatwork/cambada/blob/master/src/cambada/compile.clj
(defn- dirs->nss [dirs]
  (into []
        (comp
          (map fs/file)
          (mapcat ns-find/find-namespaces-in-dir))
        dirs))


(defn project-nss [{cp :classpath/index}]
  (-> cp :dir dirs->nss))

(u/spec-op project-nss
           (s/keys :req [:classpath/index])
           :clojure.compilation/namespaces)


(defn external-nss [{cp :classpath/index}]
  (-> cp :ext-dep dirs->nss))

(u/spec-op external-nss
           (s/keys :req [:classpath/index])
           :clojure.compilation/namespaces)


;; TODO: would be nice to have something like boot's pods to isolate
;; the current environment while compiling.
(defn compile! [{output-dir :clojure.compilation/output-dir
                 namespaces :clojure.compilation/namespaces}]
  (fs/create-directories! output-dir)
  (binding [*compile-path* output-dir]
    (doseq [n namespaces]
      (compile n))))


(u/spec-op compile!
           (s/keys :req [:clojure.compilation/output-dir
                         :clojure.compilation/namespaces]))




