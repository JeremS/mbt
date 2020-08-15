(ns fr.jeremyschoffen.mbt.alpha.core.compilation.java-test
  (:require
    [clojure.test :refer :all]
    [clojure.spec.test.alpha :as st]
    [testit.core :refer :all]
    [fr.jeremyschoffen.java.nio.alpha.file :as fs]

    [fr.jeremyschoffen.mbt.alpha.core.compilation.java :as compilation]
    [fr.jeremyschoffen.mbt.alpha.core.shell :as shell]
    [fr.jeremyschoffen.mbt.alpha.utils :as u]
    [clojure.spec.alpha :as s]))



(st/instrument `[compilation/make-java-compiler
                 compilation/make-standard-file-manager
                 compilation/make-compilation-unit
                 compilation/compile!])

;;----------------------------------------------------------------------------------------------------------------------
;; Setup
;;----------------------------------------------------------------------------------------------------------------------
(def wd (u/safer-path "resources-test" "compilation"))
(def example (u/safer-path wd "Example.java"))
(def compiled-example (u/safer-path wd "Example.class"))

(def compiler (compilation/make-java-compiler {}))
(def file-manager (compilation/make-standard-file-manager {:compilation.java/compiler compiler}))
(def compilation-unit (compilation/make-compilation-unit {:compilation.java/file-manager file-manager
                                                          :compilation.java/sources [example]}))

;;----------------------------------------------------------------------------------------------------------------------
;; Fixtures
;;----------------------------------------------------------------------------------------------------------------------
(defn compile-example! []
  (compilation/compile! #:compilation.java{:compiler compiler
                                           :file-manager file-manager
                                           :compilation-unit compilation-unit}))


(defn clean! []
  (fs/delete! compiled-example))


;;----------------------------------------------------------------------------------------------------------------------
;; Tests
;;----------------------------------------------------------------------------------------------------------------------
(defn run-program! []
  (-> {:project/working-dir wd
       :shell/command ["java" "Example"]}
      shell/safer-sh
      :out))


(deftest compilation
  (compile-example!)

  (testing "Making sure the result is there."
    (fact (fs/exists? compiled-example) => true))

  (testing "The program runs."
    (fact (run-program!) => "Message from java\n"))

  (clean!))
