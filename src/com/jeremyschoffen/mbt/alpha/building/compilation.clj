(ns com.jeremyschoffen.mbt.alpha.building.compilation
  (:require
    [clojure.spec.alpha :as s]
    [clojure.tools.namespace.find :as ns-find]
    [com.jeremyschoffen.java.nio.file :as fs]

    [com.jeremyschoffen.mbt.alpha.specs]
    [com.jeremyschoffen.mbt.alpha.building.classptah :as classpath]
    [com.jeremyschoffen.mbt.alpha.utils :as u]))


;; inspired by https://github.com/luchiniatwork/cambada/blob/master/src/cambada/compile.clj
(defn dirs->nss [dirs]
  (into []
        (comp
          (map fs/file)
          (mapcat ns-find/find-namespaces-in-dir))
        dirs))


(defn determine-nss-to-compile [{:clojure.compilation/keys [namespaces include-external-sources]
                                 :or {namespaces :all
                                      include-external-sources false}
                                 :as param}]
  (let [[type v] (s/conform :clojure.compilation/namespaces namespaces)
        cp (delay (classpath/indexed-classpath param))

        nss-to-compile (if (= type :coll)
                         v
                         (-> @cp :dir dirs->nss))]
    (-> nss-to-compile
        (cond-> include-external-sources
                (concat (-> @cp :ext-dep dirs->nss)))
        vec)))

(u/spec-op determine-nss-to-compile
           (s/keys :req [:project/working-dir
                         :project/deps]
                   :opt [:project.deps/aliases
                         :clojure.compilation/namespaces
                         :clojure.compilation/include-external-sources]))


;; TODO: would be nice to have something like boot's pods to isolate
;; the current environment while compiling.

(defn compile! [{output-dir :clojure.compilation/output-dir
                 :as param}]
  (let [nss-to-compile (determine-nss-to-compile param)]
    (fs/create-directories! output-dir)
    (binding [*compile-path* output]
      (doseq [n nss-to-compile]
        (compile n)))))

(u/spec-op compile!
           (s/keys :req [:project/working-dir
                         :clojure.compilation/output-dir
                         :project/deps]
                   :opt [:project.deps/aliases
                         :clojure.compilation/namespaces
                         :clojure.compilation/include-external-sources]))






