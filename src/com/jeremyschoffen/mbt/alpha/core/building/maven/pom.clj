(ns com.jeremyschoffen.mbt.alpha.core.building.maven.pom
  (:require
    [clojure.spec.alpha :as s]
    [clojure.java.io :as jio]
    [clojure.data.xml :as xml]
    [clojure.data.xml.tree :as tree]
    [clojure.data.xml.event :as event]
    [clojure.zip :as zip]
    [clojure.tools.deps.alpha.util.maven :as maven]
    [clojure.tools.deps.alpha.util.io :refer [printerrln]]
    [com.jeremyschoffen.java.nio.file :as fs]
    [com.jeremyschoffen.mbt.alpha.core.specs]
    [com.jeremyschoffen.mbt.alpha.core.utils :as u])
  (:import
    [java.io File Reader]
    [clojure.data.xml.node Element]))


(defn non-maven-deps [{deps-map :project/deps}]
  (into #{}
        (keep (fn [[k v]]
                (when-not (contains? v :mvn/version)
                  k)))
        (:deps deps-map)))

(u/spec-op non-maven-deps
           :param {:req [:project/deps]}
           :ret (s/coll-of symbol? :kind set?))

;; Rework from tools deps
;; https://github.com/clojure/tools.deps.alpha/blob/master/src/main/clojure/clojure/tools/deps/alpha/gen/pom.clj
(xml/alias-uri 'pom "http://maven.apache.org/POM/4.0.0")

(defn- to-dep
  [[lib {:keys [mvn/version exclusions] :as coord}]]
  (let [[group-id artifact-id classifier] (maven/lib->names lib)]
    (if version
      (cond->
        [::pom/dependency
         [::pom/groupId group-id]
         [::pom/artifactId artifact-id]
         [::pom/version version]]

        classifier
        (conj [::pom/classifier classifier])

        (seq exclusions)
        (conj [::pom/exclusions
               (map (fn [excl]
                      [::pom/exclusion
                       [::pom/groupId (or (namespace excl) (name excl))]
                       [::pom/artifactId (name excl)]])
                    exclusions)]))
      (printerrln "Skipping coordinate:" coord))))

(defn- gen-deps
  [deps]
  [::pom/dependencies
   (map to-dep deps)])

(defn- gen-source-dir
  [path]
  [::pom/sourceDirectory path])

(defn- to-repo
  [[name repo]]
  [::pom/repository
   [::pom/id name]
   [::pom/url (:url repo)]])

(defn- gen-repos
  [repos]
  [::pom/repositories
   (map to-repo repos)])


(defn filter-repos [rs]
  (remove #(= "https://repo1.maven.org/maven2/" (-> % val :url)) rs))


(defn new-pom [{project-name :maven/artefact-name
                group-id :maven/group-id
                project-version :project/version
                project-deps :project/deps}]
  (let [{deps :deps
         [path & paths] :paths
         repos :mvn/repos} project-deps
        repos (filter-repos repos)]
    (xml/sexp-as-element
      [::pom/project
       {:xmlns "http://maven.apache.org/POM/4.0.0"
        (keyword "xmlns:xsi") "http://www.w3.org/2001/XMLSchema-instance"
        (keyword "xsi:schemaLocation") "http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd"}
       [::pom/modelVersion "4.0.0"]
       [::pom/groupId group-id]
       [::pom/artifactId project-name]
       [::pom/version project-version]
       [::pom/name project-name]
       (gen-deps deps)
       (when path
         (when (seq paths) (apply printerrln "Skipping paths:" paths))
         [::pom/build (gen-source-dir path)])
       (gen-repos repos)])))

(u/spec-op new-pom
           :param {:req [:maven/artefact-name :maven/group-id :project/version :project/deps]}
           :ret :maven/pom)


(defn- make-xml-element
  [{:keys [tag attrs] :as node} children]
  (with-meta
    (apply xml/element tag attrs children)
    (meta node)))


(defn- xml-update
  [root tag-path replace-node]
  (let [z (zip/zipper xml/element? :content make-xml-element root)]
    (zip/root
      (loop [[tag & more-tags :as tags] tag-path, parent z, child (zip/down z)]
        (if child
          (if (= tag (:tag (zip/node child)))
            (if (seq more-tags)
              (recur more-tags child (zip/down child))
              (zip/edit child (constantly replace-node)))
            (recur tags parent (zip/right child)))
          (zip/append-child parent replace-node))))))


(defn- replace-info [pom i v]
  (xml-update pom [i] (xml/sexp-as-element [i v])))


(defn- replace-name  [pom name]
  (-> pom
      (replace-info ::pom/artifactId name)
      (replace-info ::pom/name name)))


(defn- replace-group-id  [pom group]
  (replace-info pom ::pom/groupId group))


(defn- replace-version [pom v]
  (replace-info pom ::pom/version (str v)))


(defn- replace-deps
  [pom deps]
  (xml-update pom [::pom/dependencies] (xml/sexp-as-element (gen-deps deps))))


(defn- replace-paths
  [pom [path & paths]]
  (when path
    (when (seq paths) (apply printerrln "Skipping paths:" paths))
    (xml-update pom [::pom/build ::pom/sourceDirectory] (xml/sexp-as-element (gen-source-dir path)))))


(defn- replace-repos
  [pom repos]
  (if (seq repos)
    (xml-update pom [::pom/repositories] (xml/sexp-as-element (gen-repos repos)))
    pom))


(defn update-pom [{pom :maven/pom
                   project-name :maven/artefact-name
                   group-id :maven/group-id
                   project-version :project/version
                   project-deps :project/deps}]
  (let [{:keys [deps paths :mvn/repos]} project-deps]
    (-> pom
        (replace-name project-name)
        (replace-group-id group-id)
        (replace-version project-version)
        (replace-deps deps)
        (replace-paths paths)
        (replace-repos repos))))

(u/spec-op update-pom
           :param {:req [:maven/pom :maven/artefact-name :maven/group-id :project/version :project/deps]}
           :ret :maven/pom)


(defn- parse-xml
  [^Reader rdr]
  (let [roots (tree/seq-tree event/event-element event/event-exit? event/event-node
                             (xml/event-seq rdr {:include-node? #{:element :characters :comment}
                                                 :skip-whitespace true}))]
    (first (filter #(instance? Element %) (first roots)))))


(defn- read-xml [path]
  (with-open [rdr (-> path fs/file jio/reader)]
    (parse-xml rdr)))


(defn pom-path [pom-dir]
  (u/safer-path pom-dir "pom.xml"))


(defn sync-pom! [{pom-dir :maven.pom/dir
                  :as      param}]
  (let [p (pom-path pom-dir)
        current-pom (when (fs/exists? p)
                      (read-xml p))]
    (if current-pom
      (spit p (-> (assoc param :maven/pom current-pom)
                  update-pom
                  xml/indent-str))
      (do
        (u/ensure-parent! p)
        (spit p (-> param new-pom xml/indent-str))))))

(u/spec-op sync-pom!
           :deps [new-pom]
           :param {:req [:maven.pom/dir
                         :maven/artefact-name
                         :maven/group-id
                         :project/version
                         :project/deps]})



(comment
  (require '[clojure.tools.deps.alpha.reader :as deps-reader])

  (def ctxt {:maven/group-id 'group
             :maven/artefact-name 'toto
             :project/version "1"
             :project/deps (deps-reader/slurp-deps "deps.edn")
             :maven.pom/dir (u/safer-path "target")})

  (def ctxt2 (assoc ctxt :project/version "2"
                         :maven/artefact-name "titi"))


  (-> ctxt
      new-pom
      xml/indent-str)

  (sync-pom! ctxt2))