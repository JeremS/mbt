(ns fr.jeremyschoffen.mbt.alpha.core.jar-test
  (:require
    [clojure.test :refer [deftest testing use-fixtures]]
    [testit.core :refer :all]

    [clojure.spec.test.alpha :as st]
    [fr.jeremyschoffen.java.nio.alpha.file :as fs]

    [fr.jeremyschoffen.mbt.alpha.core.jar :as core-jar]
    [fr.jeremyschoffen.mbt.alpha.test.helpers :as h]
    [fr.jeremyschoffen.mbt.alpha.test.repos :as repos]
    [fr.jeremyschoffen.mbt.alpha.utils :as u]))

(u/mbt-alpha-pseudo-nss
  jar
  jar.entry)

(st/instrument
  `[jar/make-jar-archive!
    jar/add-srcs!])


;;----------------------------------------------------------------------------------------------------------------------
;; Various paths
;;----------------------------------------------------------------------------------------------------------------------
(def repo repos/jar)
(def srcs (u/safer-path repo "src-dir"))
(def input-jar-src (u/safer-path repo "jar-content"))
(def input-jar-dest (u/safer-path repo "ex.jar"))
(def temp-out (u/safer-path repo "temp"))
(def jar-out (u/safer-path repo "jar.jar"))


;;----------------------------------------------------------------------------------------------------------------------
;; helpers
;;----------------------------------------------------------------------------------------------------------------------
(defn dir-content [dir]
  (-> dir
      fs/walk
      fs/realize
      (->> (into {}
                 (comp
                   (remove fs/directory?)
                   (map #(vector (str "/" (fs/relativize dir %))
                                 (slurp %))))))))

(defn- delete-dir [dir]
  (doseq [f (-> dir
                fs/walk
                fs/realize
                rseq)]
    (fs/delete-if-exists! f)))


;;----------------------------------------------------------------------------------------------------------------------
;; Jar sources
;;----------------------------------------------------------------------------------------------------------------------
(def text-entries [{::jar.entry/src "some text."
                    ::jar.entry/dest (fs/path "example.txt")}
                   {::jar.entry/src "some text 2."
                    ::jar.entry/dest (fs/path "example2.txt")}])


(def text-entries-representation
  {"/example.txt" "some text."
   "/example2.txt" "some text 2."})


(defn exclude-text-entries [entry]
  (= "example2.txt"
     (-> entry
         ::jar.entry/dest
         fs/file-name
         str)))


(defn jar-exclude [entry]
  (= "jarfile-1.txt"
     (-> entry
         ::jar.entry/src
         fs/file-name
         str)))


(def jar-sources
  [(core-jar/to-entries text-entries exclude-text-entries)
   srcs
   (core-jar/to-entries input-jar-dest jar-exclude)])


;;----------------------------------------------------------------------------------------------------------------------
;; Fixtures
;;----------------------------------------------------------------------------------------------------------------------
(defn make-input-jar! []
  (core-jar/make-jar-archive! {::jar/temp-output input-jar-src
                               ::jar/output      input-jar-dest}))

(defn clean-input-jar! []
  (fs/delete-if-exists! input-jar-dest))


(defn make-temp-out! []
  (core-jar/add-srcs! {::jar/srcs       jar-sources
                       ::jar/temp-output temp-out}))


(defn clean-temp-out! []
  (delete-dir temp-out))


(defn make-jar! []
  (core-jar/make-jar-archive! {::jar/temp-output temp-out
                               ::jar/output jar-out}))

(defn clean-jar! []
  (fs/delete-if-exists! jar-out))


(defn setup! []
  (make-input-jar!)
  (make-temp-out!)
  (make-jar!))


(defn tear-down! []
  (clean-jar!)
  (clean-temp-out!)
  (clean-input-jar!))


(defn fixtures [f]
  (setup!)
  (f)
  (tear-down!))


(use-fixtures :once fixtures)
;;----------------------------------------------------------------------------------------------------------------------
;; Actual tests
;;----------------------------------------------------------------------------------------------------------------------


(deftest make-archive
  (testing "Making sure the input jar is correctly made"
    (fact
      (dir-content input-jar-src) => (h/jar-content input-jar-dest))))


(def jar-content
  (dissoc (merge text-entries-representation
                 (dir-content input-jar-src)
                 (dir-content srcs))
          "/jarfile-1.txt"
          "/example2.txt"))


(deftest add-srcs
  (testing "Testing the result of add-srcs in temp output"
    (fact
      jar-content => (dir-content temp-out)))

  (testing "Testing the result of add-srcs in the archive"
    (fact
      jar-content => (h/jar-content jar-out))))
