(ns com.jeremyschoffen.mbt.alpha.building.pom
  (:require
    [clojure.data.xml :as xml]
    [clojure.data.xml.tree :as tree]
    [clojure.data.xml.event :as event]
    [clojure.java.io :as io]
    [clojure.spec.alpha :as s]
    [clojure.tools.deps.alpha.gen.pom :as deps-pom]
    [com.jeremyschoffen.mbt.alpha.specs]
    [com.jeremyschoffen.mbt.alpha.utils :as u]
    [clojure.tools.deps.alpha.reader :as deps-reader]
    [com.jeremyschoffen.java.nio.file :as fs])
  (:import [java.io Reader]
           [clojure.data.xml.node Element]))


(defn non-maven-deps [{deps-map :project/deps}]
  (into #{}
        (keep (fn [[k v]]
                (when-not (contains? v :mvn/version)
                  k)))
        (:deps deps-map)))

(u/spec-op non-maven-deps
           (s/keys :req [:project/deps])
           (s/coll-of symbol? :kind set?))


(defn sync-deps-pom! [{pom-dir  :maven.pom/dir
                       deps-map :project/deps}]
  (deps-pom/sync-pom deps-map (fs/file pom-dir)))

(u/spec-op sync-deps-pom!
           (s/keys :req [:maven.pom/dir]))


(xml/alias-uri 'pom "http://maven.apache.org/POM/4.0.0")


(defn pom-path [pom-dir]
  (u/safer-path pom-dir "pom.xml"))

;;----------------------------------------------------------------------------------------------------------------------
;; taken from clojure.tools.deps.alpha.gen.pom
(defn- parse-xml
  [^Reader rdr]
  (let [roots (tree/seq-tree event/event-element event/event-exit? event/event-node
                             (xml/event-seq rdr {:include-node? #{:element :characters :comment}
                                                 :skip-whitespace true}))]
    (first (filter #(instance? Element %) (first roots)))))
;;----------------------------------------------------------------------------------------------------------------------

(defn- read-xml [path]
  (with-open [rdr (-> path fs/file io/reader)]
    (parse-xml rdr)))




(def ^:private replaced-tags #{::pom/groupId
                               ::pom/artifactId
                               ::pom/version})

(defn- make-replecment-nodes [{artefact-name :artefact/name
                               version :project/version
                               group-id :maven/group-id}]
  [(xml/sexp-as-element [::pom/groupId   group-id])
   (xml/sexp-as-element [::pom/artifactId artefact-name])
   (xml/sexp-as-element [::pom/version  (str version)])])


(defn- update-pom [param xml-root]
  (update xml-root :content
          (fn [nodes]
            (concat (make-replecment-nodes param)
                    (remove #(contains? replaced-tags (:tag %)) nodes)))))


(defn new-pom [{pom-dir  :maven.pom/dir
                :as param}]
  (let [p (pom-path pom-dir)
        xml-root (read-xml p)]
    (update-pom param xml-root)))

(u/spec-op new-pom
           (s/keys :req [:maven.pom/dir
                         :artefact/name
                         :project/version
                         :maven/group-id])
           :maven/pom)


(defn sync-pom! [{pom-dir :maven.pom/dir
                  pom :maven/pom}]
  (spit (pom-path pom-dir)
        (xml/indent-str pom)))

(u/spec-op sync-pom!
           (s/keys :req [:maven.pom/dir :maven/pom]))

(comment
  (require '[com.jeremyschoffen.java.nio.file :as fs])
  (require '[clojure.tools.deps.alpha.reader :as deps-reader])
  (fs/delete-if-exists! "./pom.xml")
  (sync-deps-pom! {:project/deps (deps-reader/slurp-deps "deps.edn")
                   :maven.pom/dir (u/safer-path"./target")})

  (-> {:maven.pom/dir (u/safer-path "./target")
       :project/deps (deps-reader/slurp-deps "deps.edn")
       :artefact/name "project"
       :project/version "2.12"
       :maven/group-id "super"}
      (u/side-effect sync-deps-pom!)
      (u/assoc-computed :maven/pom new-pom)
      (u/side-effect sync-pom!)))

