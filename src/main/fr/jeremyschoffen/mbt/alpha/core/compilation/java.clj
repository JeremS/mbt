(ns ^{:author "Jeremy Schoffen"
      :doc "
Minimal api wrapping some of the `java.tools` apis providing java compilation utilities.
      "}
  fr.jeremyschoffen.mbt.alpha.core.compilation.java
  (:require
    [clojure.spec.alpha :as s]
    [fr.jeremyschoffen.java.nio.alpha.file :as fs]

    [fr.jeremyschoffen.mbt.alpha.core.jar.fs :as jar-fs]
    [fr.jeremyschoffen.mbt.alpha.core.specs :as specs]
    [fr.jeremyschoffen.mbt.alpha.utils :as u])
  (:import
    (javax.tools StandardJavaFileManager JavaCompiler ToolProvider)))


;;----------------------------------------------------------------------------------------------------------------------
;; Finding java files
;;----------------------------------------------------------------------------------------------------------------------
(defn- find-java-files* [src]
  (-> src
      fs/walk
      fs/realize
      (->> (filter #(= "java" (fs/file-extention %))))))

(u/simple-fdef find-java-files*
               specs/dir-path?
               (s/coll-of fs/path?))


(defn project-files
  "Use an indexed classpath to find all .java files src directories from inside the working directory.

  See:
  - [[fr.jeremyschoffen.mbt.alpha.core.classpath/indexed-classpath]]"
  [{ i :classpath/index}]
  (-> i
      :classpath/dir
      (->> (into []
                 (comp
                   (map u/safer-path)
                   (mapcat find-java-files*))))))

(u/spec-op project-files
           :param {:req [:classpath/index]}
           :ret :compilation.java/sources)


(defn external-files
  "Use an indexed classpath to find all .java files from src directories located outside the working directory.\n  These would be namespaces from local deps or directly from a git repo.

  See:
  - [[fr.jeremyschoffen.mbt.alpha.core.classpath/indexed-classpath]]."
  [{ i :classpath/index}]
  (-> i
      :classpath/ext-dep
      (->> (into []
                 (mapcat find-java-files*)))))

(u/spec-op external-files
           :param {:req [:classpath/index]}
           :ret :compilation.java/sources)


(defn jar-files [{ i :classpath/index}]
  "Use an indexed classpath to find all .java files from jars.

  See:
    - [[fr.jeremyschoffen.mbt.alpha.core.classpath/indexed-classpath]]
  "
  (-> i
      :classpath/jar
      (->> (into []
                 (mapcat (fn [src-jar]
                           (with-open [src (jar-fs/read-only-jar-fs src-jar)]
                             (find-java-files* src))))))))

(u/spec-op jar-files
           :param {:req [:classpath/index]}
           :ret :compilation.java/sources)
;;----------------------------------------------------------------------------------------------------------------------
;; Building java object used to compile.
;;----------------------------------------------------------------------------------------------------------------------
(defn make-java-compiler
  "Return the platform's default java compiler."
  {:tag JavaCompiler}
  [& _]
  (ToolProvider/getSystemJavaCompiler))

(u/spec-op make-java-compiler
           :ret :compilation.java/compiler)


(defn make-standard-file-manager
  "Make a standard file manager from a java compiler."
  {:tag StandardJavaFileManager}
  [{compiler :compilation.java/compiler
    opts :compilation.java.file-manager/options}]
  (let [{:compilation.java.file-manager/keys [listener locale charset]} opts]
    (.getStandardFileManager ^JavaCompiler compiler listener locale charset)))

(u/spec-op make-standard-file-manager
           :param {:req [:compilation.java/compiler]
                   :opt [:compilation.java.file-manager/options]})


(defn make-compilation-unit
  "Make a compilation unit to give a compilation task using a file manager."
  [{file-manager :compilation.java/file-manager
    sources :compilation.java/sources}]
  (->> sources
       (mapv fs/file)
       (.getJavaFileObjectsFromFiles ^StandardJavaFileManager file-manager)))

(u/spec-op make-compilation-unit
           :param {:req [:compilation.java/file-manager
                         :compilation.java/sources]})

;;----------------------------------------------------------------------------------------------------------------------
;; Compilation proper
;;----------------------------------------------------------------------------------------------------------------------
(defn compile!
  "Compile java files. This function makes a compilation task with `compiler.getTask` and immediatly calls `.call on
  it`.

  See: `javax.tool.JavaCompiler`."
  [{compiler            :compilation.java/compiler
    file-manager        :compilation.java/file-manager
    out                 :compilation.java/compiler-out
    diagnostic-listener :compilation.java/diagnostic-listener
    options             :compilation.java/options
    classes             :compilation.java/compiler-classes
    compilation-unit    :compilation.java/compilation-unit}]
  (.call (.getTask ^JavaCompiler compiler
                   out
                   file-manager
                   diagnostic-listener
                   options
                   classes
                   compilation-unit)))

(u/spec-op compile!
           :param {:req [:compilation.java/compiler
                         :compilation.java/file-manager
                         :compilation.java/compilation-unit]
                   :opt [:compilation.java/compiler-out
                         :compilation.java/diagnostic-listener
                         :compilation.java/options
                         :compilation.java/compiler-classes]})
