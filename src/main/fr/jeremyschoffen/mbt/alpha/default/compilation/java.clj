(ns ^{:author "Jeremy Schoffen"
      :doc "
Higher level api to compile java files.
      "}
  fr.jeremyschoffen.mbt.alpha.default.compilation.java
  (:require
    [clojure.spec.alpha :as s]
    [fr.jeremyschoffen.mbt.alpha.core :as mbt-core]
    [fr.jeremyschoffen.mbt.alpha.utils :as u]))


(defn default-options
  "Make a vector of options to be used when making a compilation task.

  Arg keys:
  - `:classpath/raw-absolute`: used to fill the `-cp` option of the compiler
  - `:compilation.java/output-dir`: used to fill the `-d` option of the compiler allowing to choose
    where the compiler outputs .class files."
  [{classpath-abs :classpath/raw-absolute
    dest-dir  :compilation.java/output-dir}]
  (cond-> []
          classpath-abs (conj "-cp" (str classpath-abs))
          dest-dir (conj "-d" (str dest-dir))))

(u/spec-op default-options
           :param {:opt [:classpath/raw-absolute
                         :compilation.java/output-dir]}
           :ret :compilation.java/options)


(defn ensure-compilation-args
  "Ensures several `:compilation.java/XXX` keys.

  - `:compilation.java/compiler`: provides a compiler using
    [[fr.jeremyschoffen.mbt.alpha.core/compilation-java-compiler]]
  - `:compilation.java/file-manager`: provides a file manager using
    [[fr.jeremyschoffen.mbt.alpha.core/compilation-java-std-file-manager]]
  - `:compilation.java/options`: prodide compiler options using
    [[fr.jeremyschoffen.mbt.alpha.default.compilation.java/default-options]]
  - `:compilation.java/sources`: provide the project java sources using
    [[fr.jeremyschoffen.mbt.alpha.core/compilation-java-project-files]]
  - `:compilation.java/compilation-unit`: make a compilation unit from the sources with
    [[fr.jeremyschoffen.mbt.alpha.core/compilation-java-unit]]"
  [param]
  (u/ensure-computed param
                     :classpath/raw-absolute mbt-core/classpath-raw-absolute
                     :compilation.java/compiler mbt-core/compilation-java-compiler
                     :compilation.java/file-manager mbt-core/compilation-java-std-file-manager
                     :compilation.java/options default-options
                     :compilation.java/sources mbt-core/compilation-java-project-files
                     :compilation.java/compilation-unit mbt-core/compilation-java-unit))

(u/spec-op ensure-compilation-args
           :deps [mbt-core/compilation-java-compiler
                  mbt-core/compilation-java-std-file-manager
                  default-options
                  mbt-core/compilation-java-project-files
                  mbt-core/compilation-java-unit]
           :param {:opt [:compilation.java/compiler
                         :compilation.java/file-manager
                         :compilation.java/output-dir
                         :compilation.java/options
                         :classpath/index]}
           :ret (s/keys :req [:compilation.java/compiler
                              :compilation.java/file-manager
                              :compilation.java/options
                              :compilation.java/sources
                              :compilation.java/compilation-unit]))


(defn compile!
  "Compile java files. Same as [[fr.jeremyschoffen.mbt.alpha.core/compile-java!]] with several parameters ensured by
  default using [[fr.jeremyschoffen.mbt.alpha.default.compilation.java/ensure-compilation-args]]."
  [param]
  (-> param
      ensure-compilation-args
      mbt-core/compile-java!))

(u/spec-op compile!
           :deps [ensure-compilation-args mbt-core/compile-java!]
           :param {:opt [:classpath/index
                         :classpath/raw-absolute
                         :compilation.java/compilation-unit
                         :compilation.java/compiler
                         :compilation.java/compiler-classes
                         :compilation.java/compiler-out
                         :compilation.java/diagnostic-listener
                         :compilation.java/file-manager
                         :compilation.java/options
                         :compilation.java/output-dir]})

(u/param-suggestions compile!)