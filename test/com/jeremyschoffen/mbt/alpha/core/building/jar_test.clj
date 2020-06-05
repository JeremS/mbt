(ns com.jeremyschoffen.mbt.alpha.core.building.jar-test
  (:require
    [clojure.test :refer [deftest testing]]
    [clojure.spec.test.alpha :as spec-test]
    [clojure.edn :as edn]
    [testit.core :refer :all]
    [com.jeremyschoffen.java.nio.file :as fs]
    [com.jeremyschoffen.mbt.alpha.core.building.classpath :as cp]
    [com.jeremyschoffen.mbt.alpha.core.building.cleaning :as clean]
    [com.jeremyschoffen.mbt.alpha.core.building.deps :as deps]
    [com.jeremyschoffen.mbt.alpha.core.building.maven.pom :as pom]
    [com.jeremyschoffen.mbt.alpha.core.building.jar :as jar]
    [com.jeremyschoffen.mbt.alpha.core.helpers.test-repos :as test-repos]
    [com.jeremyschoffen.mbt.alpha.core.specs]
    [com.jeremyschoffen.mbt.alpha.core.utils :as u]))


(spec-test/instrument)

;;----------------------------------------------------------------------------------------------------------------------
;; helpers
;;----------------------------------------------------------------------------------------------------------------------
(defn make-jar! [{wd :project/working-dir
                  :as param}]
  (let [target (u/ensure-dir! (u/safer-path wd "target"))
        temp-jar-dir (fs/create-temp-directory! "jar_" {:dir target})
        res (atom nil)]
    (-> param
        (assoc :jar/temp-output temp-jar-dir
               :cleaning/target temp-jar-dir)
        (u/side-effect! #(reset! res (jar/add-srcs! %)))
        (u/side-effect! jar/make-jar-archive!)
        (u/side-effect! clean/clean!))
    @res))

;; uberjar without clojure.
(defn- uber-jar-srcs [param]
  (->> param
       jar/uber-jar-srcs
       (into []
             (remove (fn [v]
                       (and (fs/path? v)
                            (->> v
                                 str
                                 (re-find #"/org/clojure/"))))))))


(defn add-service-prop-entry [srcs p]
  (let [name (str (fs/file-name p))]
    (conj srcs
          [{:jar.entry/src p
            :jar.entry/dest (fs/path "META-INF" "services" name)}])))


(defn jar-content [jar-path]
  (with-open [zfs (jar/open-jar-fs jar-path)]
    (->> zfs
         fs/walk
         fs/realize
         (into {}
               (comp
                 (remove fs/directory?)
                 (map #(vector (str %) (slurp %))))))))
;;----------------------------------------------------------------------------------------------------------------------
;; Common values
;;----------------------------------------------------------------------------------------------------------------------


(def version "1.0")
(def group-id "group")
(def services-props-rpath (fs/path "resources" "META-INF" "services" "services.properties"))


;;----------------------------------------------------------------------------------------------------------------------
;; Test skinny jar using project 2
;;----------------------------------------------------------------------------------------------------------------------
(def project2-path test-repos/monorepo-p2)
(def project2-target-path (u/safer-path project2-path "target"))
(def project2-jar (u/safer-path project2-target-path "project2.jar"))
(def artefact-name2 "project-2")

(def ctxt2
  (-> {:project/working-dir project2-path
       :artefact/name artefact-name2
       :project/version version
       :maven/group-id (symbol group-id)
       :project/author "Tester"

       :jar/output project2-jar
       :cleaning/target project2-target-path}

      (u/assoc-computed
        :project/deps deps/get-deps
        :classpath/index cp/indexed-classpath
        :maven/pom pom/new-pom)))


(defn jar2! []
  (-> ctxt2
      (u/assoc-computed :jar/srcs jar/simple-jar-srcs)
      make-jar!))


(deftest simple-jar
  (let [_ (jar2!)
        content (jar-content project2-jar)]

    (facts
      (get content "/project2/core.clj")
      => (slurp (u/safer-path project2-path "src" "project2" "core.clj"))

      (edn/read-string (get content "/META-INF/deps/group/project-2/deps.edn"))
      => (edn/read-string (slurp (u/safer-path project2-path "deps.edn")))

      (edn/read-string (get content "/data_readers.cljc"))
      => (edn/read-string (slurp (u/safer-path project2-path "src" "data_readers.cljc")))

      (get content "/META-INF/services/services.properties")
      => (slurp (u/safer-path project2-path services-props-rpath)))

    (clean/clean! ctxt2)))

;;----------------------------------------------------------------------------------------------------------------------
;; Testing uber jar using project 1
;;----------------------------------------------------------------------------------------------------------------------
(def project1-path test-repos/monorepo-p1)
(def project1-target-path (u/safer-path project1-path "target"))
(def project1-uberjar (u/safer-path project1-target-path "project1-standalone.jar"))
(def artefact-name1 "project-1")

(defn get-project1-deps [ctxt]
  (-> ctxt
      (deps/get-deps)
      (assoc-in [:deps 'project2/project2 :local/root] (str project2-path))))


(def ctxt1
  (-> {:project/working-dir project1-path
       :artefact/name artefact-name1
       :project/version version
       :maven/group-id (symbol group-id)
       :project/author "Tester"

       :jar/output project1-uberjar
       :cleaning/target project1-target-path}

      (u/assoc-computed
        :project/deps get-project1-deps
        :classpath/index cp/indexed-classpath
        :maven/pom pom/new-pom)))


(defn uberjar1! []
  (-> ctxt1
      (u/assoc-computed :jar/srcs uber-jar-srcs)
      make-jar!))


(deftest uberjar
  (let [_ (uberjar1!)
        content (jar-content project1-uberjar)
        services-1 (slurp (u/safer-path project1-path services-props-rpath))
        services-2 (slurp (u/safer-path project2-path services-props-rpath))]

    (facts
     (get content "/project1/core.clj")
     => (slurp (u/safer-path project1-path "src" "project1" "core.clj"))

     (get content "/project2/core.clj")
     => (slurp (u/safer-path project2-path "src" "project2" "core.clj"))


     (edn/read-string (get content "/data_readers.cljc"))
     => (merge (-> (u/safer-path project1-path "src" "data_readers.cljc")
                   slurp
                   edn/read-string)
               (-> (u/safer-path project2-path "src" "data_readers.cljc")
                   slurp
                   edn/read-string))

     (or (= (get content "/META-INF/services/services.properties")
            (str services-1 "\n" services-2 "\n"))
         (= (get content "/META-INF/services/services.properties")
            (str services-2 "\n" services-1 "\n")))
     => true)
    (clean/clean! ctxt1)))
