(ns ^{:author "Jeremy Schoffen"
      :doc "
Api providing maven pom.xml files generation.
      "}
  fr.jeremyschoffen.mbt.alpha.core.maven.pom
  (:require
    [clojure.java.io :as jio]
    [clojure.data.xml :as xml]
    [clojure.data.xml.tree :as tree]
    [clojure.data.xml.event :as event]
    [clojure.zip :as zip]
    [clojure.tools.deps.alpha.util.maven :as tools-maven]
    [clojure.tools.deps.alpha.util.io :refer [printerrln]]
    [cognitect.anomalies :as anom]
    [fr.jeremyschoffen.java.nio.alpha.file :as fs]
    [fr.jeremyschoffen.mbt.alpha.core.specs]
    [fr.jeremyschoffen.mbt.alpha.utils :as u])
  (:import
    [java.io Reader]
    [clojure.data.xml.node Element]))


(u/pseudo-nss
  project
  project.license
  maven
  maven.pom
  maven.scm)



;; Rework from tools deps
;; https://github.com/clojure/tools.deps.alpha/blob/master/src/main/clojure/clojure/tools/deps/alpha/gen/pom.clj

(xml/alias-uri 'pom "http://maven.apache.org/POM/4.0.0")


;;----------------------------------------------------------------------------------------------------------------------
;; Generation
;;----------------------------------------------------------------------------------------------------------------------

(defn- maybe-tag
  ([n v]
   (maybe-tag n v identity))
  ([n v f]
   (when v
     [n (f v)])))


(defn- gen-license [license]
  (let [{license-name ::project.license/name
         ::project.license/keys [url distribution comment]} license]
    (into [::pom/license]
          (keep identity)
          [(maybe-tag ::pom/name license-name)
           (maybe-tag ::pom/url url)
           (maybe-tag ::pom/distribution distribution name)
           (maybe-tag ::pom/comment comment)])))


(defn- gen-licenses [licenses]
  (into [::pom/licenses] (map gen-license) licenses))


(defn- gen-scm [maven-scm]
  (let [{::maven.scm/keys [connection developer-connection tag url]} maven-scm]
    (into [::pom/scm]
          (keep identity)
          [(maybe-tag ::pom/connection connection)
           (maybe-tag ::pom/developerConnection developer-connection)
           (maybe-tag ::pom/tag tag)
           (maybe-tag ::pom/url url)])))


(defn- to-dep
  [[lib {:keys [mvn/version exclusions] :as coord}]]
  (let [[group-id artifact-id classifier] (tools-maven/lib->names lib)]
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


(defn- filter-repos [rs]
  (remove #(= "https://repo1.maven.org/maven2/" (-> % val :url)) rs))


(defn new-pom
  "Make a fresh pom using the tree of maps representation used in the `clojure.data.xml` library."
  [{project-name ::maven/artefact-name
    group-id ::maven/group-id
    project-version ::project/version
    project-deps ::project/deps
    maven-scm ::maven/scm
    licenses ::project/licenses}]
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
       (when licenses
         (gen-licenses licenses))
       (when maven-scm
         (gen-scm maven-scm))
       (gen-deps deps)
       (when path
         (when (seq paths) (apply printerrln "Skipping paths:" paths))
         [::pom/build (gen-source-dir path)])
       (gen-repos repos)])))

(u/spec-op new-pom
           :param {:req [::maven/artefact-name
                         ::maven/group-id
                         ::project/version
                         ::project/deps]
                   :opt [::maven/scm
                         ::project/licenses]}
           :ret ::maven.pom/xml)

;;----------------------------------------------------------------------------------------------------------------------
;; Update
;;----------------------------------------------------------------------------------------------------------------------
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
            (if-let [next-sibling (zip/right child)]
              (recur tags parent next-sibling)
              (if (seq more-tags)
                (let [new-parent (zip/append-child parent (xml/sexp-as-element tag))
                      new-child (zip/rightmost (zip/down new-parent))]
                  (recur more-tags new-child (zip/down new-child)))
                (zip/append-child parent replace-node))))
          (if (seq more-tags)
            (let [new-parent (zip/append-child parent (xml/sexp-as-element tag))
                  new-child (zip/rightmost (zip/down new-parent))]
              (recur more-tags new-child (zip/down new-child)))
            (zip/append-child parent replace-node)))))))

(defn- replace-info [pom i v]
  (xml-update pom [i] (xml/sexp-as-element [i v])))


(defn replace-licenses [pom licenses]
  (xml-update pom [::pom/licenses] (xml/sexp-as-element (gen-licenses licenses))))


(defn replace-scm [pom scm]
  (xml-update pom [::pom/scm] (xml/sexp-as-element (gen-scm scm))))


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
  (if path
    (do
      (when (seq paths) (apply printerrln "Skipping paths:" paths))
      (xml-update pom [::pom/build ::pom/sourceDirectory] (xml/sexp-as-element (gen-source-dir path))))
    pom))


(defn- replace-repos
  [pom repos]
  (if (seq repos)
    (xml-update pom [::pom/repositories] (xml/sexp-as-element (gen-repos repos)))
    pom))


(defn- update-pom [{pom ::maven.pom/xml
                    project-name ::maven/artefact-name
                    group-id ::maven/group-id
                    project-version ::project/version
                    project-deps ::project/deps
                    scm ::maven/scm
                    licenses ::project/licenses}]
  (let [{:keys [deps paths :mvn/repos]} project-deps]
    (-> pom
        (replace-name project-name)
        (replace-group-id group-id)
        (replace-version project-version)
        (replace-licenses licenses)
        (replace-scm scm)
        (replace-deps deps)
        (replace-paths paths)
        (replace-repos repos))))

(u/spec-op update-pom
           :param {:req [::maven.pom/xml
                         ::maven/artefact-name
                         ::maven/group-id
                         ::project/version
                         ::project/deps]
                   :opt [::maven/scm
                         ::project/licenses]}
           :ret ::maven.pom/xml)


;;----------------------------------------------------------------------------------------------------------------------
;; Sync
;;----------------------------------------------------------------------------------------------------------------------
(defn- parse-xml
  [^Reader rdr]
  (let [roots (tree/seq-tree event/event-element event/event-exit? event/event-node
                             (xml/event-seq rdr {:include-node? #{:element :characters :comment}
                                                 :skip-whitespace true}))]
    (first (filter #(instance? Element %) (first roots)))))


(defn- read-xml [path]
  (with-open [rdr (-> path fs/file jio/reader)]
    (parse-xml rdr)))


(defn read-pom
  "Read a `pom.xml` file at `:fr...mbt.alpha.maven.pom/path` and turns it into xml data."
  [{pom-path ::maven.pom/path}]
  (if (fs/exists? pom-path)
    (read-xml pom-path)
    (throw (ex-info "Pom file doesn't exist."
                    {::anom/category ::anom/not-found
                     :path pom-path}))))

(u/spec-op read-pom
           :param {:req [::maven.pom/path]}
           :ret ::maven.pom/xml)


(defn sync-pom!
  "Function similar to `clojure.tools.deps.alpha.gen.pom/sync-pom` with added behaviour.
  This function fills the maven coordinates for the project, the licenses and scm parts.

  Also returns the map version of the synced xml."
  [{pom-path ::maven.pom/path
    :as      param}]
  (u/ensure-parent! pom-path)
  (let [xml (if (fs/exists? pom-path)
              (-> param
                  (u/assoc-computed ::maven.pom/xml read-pom)
                  update-pom)
              (new-pom param))]
    (->> xml
         xml/indent-str
         (spit pom-path))))

(u/spec-op sync-pom!
           :deps [read-pom
                  update-pom
                  new-pom]
           :param {:req [::maven/artefact-name
                         ::maven/group-id
                         ::maven.pom/path
                         ::project/deps
                         ::project/version]
                   :opt [::maven/scm
                         ::project/licenses]}
           :ret ::maven.pom/xml)

;; inspired by https://github.com/seancorfield/depstar/blob/develop/src/hf/depstar/uberjar.clj#L322
(defn new-pom-properties
  "Makes the text of the pom.properties file found in jars."
  [{group-id ::maven/group-id
    artefact-name ::maven/artefact-name
    v ::project/version}]
  (let [now (java.util.Date.)]
    (str "#Generated by Mbt\n"
         "#" now "\n"
         "version: " v "\n"
         "groupId: " group-id "\n"
         "artifactId: " artefact-name "\n")))

(u/spec-op new-pom-properties
           :param {:req [::maven/group-id
                         ::maven/artefact-name
                         ::project/version]}
           :ret ::maven.pom/properties)


(comment
  'clojure.tools.deps.alpha.gen.pom
  (require '[clojure.tools.deps.alpha :as deps-reader])

  (def ctxt {::maven/group-id 'group
             ::maven/artefact-name 'toto
             ::project/version "1"
             ::project/deps (deps-reader/slurp-deps (fs/file "deps.edn"))
             ::maven.pom/path (u/safer-path "target" "pom.test.xml")

             ::project/licenses [{::project.license/name "toto"
                                  ::project.license/url "www.toto.com"
                                  ::project.license/distribution :repo}]

             ::maven/scm {::maven.scm/connection "scm:svn:http://127.0.0.1/svn/my-project"
                          ::maven.scm/developer-connection "scm:svn:https://127.0.0.1/svn/my-project"
                          ::maven.scm/tag "HEAD"
                          ::maven.scm/url "http://127.0.0.1/websvn/my-project"}})

  (def ctxt2 (assoc ctxt :project/version "2"
                         :maven/artefact-name "titi"))

  (-> ctxt
      sync-pom!)

  (-> ctxt2
      new-pom
      xml/indent-str))
