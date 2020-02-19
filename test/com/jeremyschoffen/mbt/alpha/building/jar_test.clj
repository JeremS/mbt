(ns com.jeremyschoffen.mbt.alpha.building.jar-test
  (:require
    [clojure.test :refer [deftest testing]]
    [clojure.spec.test.alpha :as spec-test]
    [clojure.edn :as edn]
    [clojure.tools.deps.alpha.reader :as deps-reader]
    [testit.core :refer :all]
    [com.jeremyschoffen.java.nio.file :as fs]
    [com.jeremyschoffen.mbt.alpha.building.building-utils :as bu]
    [com.jeremyschoffen.mbt.alpha.building.classpath :as cp]
    [com.jeremyschoffen.mbt.alpha.building.deps :as deps]
    [com.jeremyschoffen.mbt.alpha.building.pom :as pom]
    [com.jeremyschoffen.mbt.alpha.building.jar :as jar]
    [com.jeremyschoffen.mbt.alpha.specs]
    [com.jeremyschoffen.mbt.alpha.utils :as u]
    [com.jeremyschoffen.mbt.alpha.helpers_test :as h]))


(spec-test/instrument)


(defn make-context [project-path]
  (-> {:project/working-dir project-path
       :project/version "0.1.1"
       :maven/group-id 'mbt
       :artefact/name (str (fs/file-name project-path))}
      (u/assoc-computed :project/deps deps/get-deps)
      (u/assoc-computed :classpath/index cp/indexed-classpath)
      (u/assoc-computed :maven/pom pom/new-pom)))


(defn make-temp-jar-out [project-path n]
  (let [p (fs/path project-path "target" n)]
    (fs/create-directories! p)
    p))


(defn make-temp-jar-path [project-path]
  (make-temp-jar-out project-path "temp-jar"))


(defn make-jar-path [project-path]
  (fs/path project-path "target" (-> project-path fs/file-name (str ".jar"))))


(defn simple-jar-ctxt [wd]
  (-> wd
      make-context
      (assoc :jar/temp-output (make-temp-jar-path wd))
      (assoc :jar/output (make-jar-path wd))
      (u/assoc-computed :jar/srcs jar/simple-jar-srcs)))


(defn make-jar! [ctxt]
  (-> ctxt
      (u/side-effect! jar/add-srcs!)
      (u/side-effect! jar/jar!)))


(defn explore-simple-jar [ctxt]
  (with-open [zfs (jar/make-output-jar-fs ctxt)]
    (->> zfs
         fs/walk
         fs/realize
         (into {}
               (comp
                 (remove fs/directory?)
                 (map (fn [p]
                        [(-> p fs/file-name str)
                         (slurp p)])))))))


(deftest simple-jars
  (let [repo (bu/make-test-project)
        ctxt2 (simple-jar-ctxt (u/safer-path repo bu/project2))
        _ (make-jar! ctxt2)
        content2 (explore-simple-jar ctxt2)]

    (facts
      (get content2 "core.clj")
      => (slurp (bu/test-resource-path "core_p2.clj"))

      (edn/read-string (get content2 "deps.edn"))
      => (edn/read-string (slurp (bu/test-resource-path "deps-p2.edn")))

      (edn/read-string (get content2 "data_readers.cljc"))
      => (edn/read-string (slurp (bu/test-resource-path "data_readers-p2.cljc"))))))


(defn make-temp-uberjar-path [project-path]
  (make-temp-jar-out project-path "temp-uber-jar"))


(defn make-uberjar-path [project-path]
  (fs/path project-path "target" (-> project-path fs/file-name (str "-standalone.jar"))))


(defn explore-uber-jar [ctxt]
  (with-open [zfs (jar/make-output-jar-fs ctxt)]
    (->> zfs
         fs/walk
         fs/realize
         (into {}
               (comp
                 (remove fs/directory?)
                 (map (fn [p]
                        [(str p)
                         (slurp p)])))))))

;; TODO: Test META-INF/services clash
(deftest uberjar
  (let [repo (bu/make-test-project)
        wd (u/safer-path repo bu/project1)
        ctxt1 (-> wd
                  make-context
                  (assoc :jar/temp-output (make-temp-uberjar-path wd))
                  (assoc :jar/output (make-uberjar-path wd))
                  (u/assoc-computed :jar/srcs jar/uber-jar-srcs)
                  (update :jar/srcs #(remove (fn [src]
                                               (and (fs/path? src)
                                                    (re-find #"org/clojure" (str src))))
                                             %)))
        _ (make-jar! ctxt1)
        content (explore-uber-jar ctxt1)]

    (facts
      (get content "/project1/core.clj")
      => (slurp (bu/test-resource-path "core_p1.clj"))

      (get content "/project2/core.clj")
      => (slurp (bu/test-resource-path "core_p2.clj"))


      (edn/read-string (get content "/data_readers.cljc"))
      => (merge (-> "data_readers-p1.cljc" bu/test-resource-path slurp edn/read-string)
                (-> "data_readers-p2.cljc" bu/test-resource-path slurp edn/read-string)))))
