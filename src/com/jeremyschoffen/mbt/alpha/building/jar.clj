(ns com.jeremyschoffen.mbt.alpha.building.jar
  (:require
    [clojure.data.xml :as xml]
    [clojure.edn :as edn]
    [clojure.spec.alpha :as s]
    [clojure.set :as c-set]
    [com.jeremyschoffen.java.nio.file :as fs]
    [com.jeremyschoffen.mbt.alpha.building.manifest :as manifest]
    [com.jeremyschoffen.mbt.alpha.building.pom :as pom]
    [com.jeremyschoffen.mbt.alpha.specs :as specs]
    [com.jeremyschoffen.mbt.alpha.utils :as u])
  (:import
    (java.nio.file FileSystem)
    (java.net URI)
    (java.util HashMap)))

;; TODO: decide what to do with wayward files on a classpath.

;;----------------------------------------------------------------------------------------------------------------------
;; Utils
;;----------------------------------------------------------------------------------------------------------------------
(defn ensure-dir! [d]
  (when (fs/not-exists? (fs/create-directories! d)))
  d)


(defn ensure-parent! [f]
  (-> f fs/parent ensure-dir!)
  f)

;;----------------------------------------------------------------------------------------------------------------------
;; Jar FileSystem cstr
;;----------------------------------------------------------------------------------------------------------------------
(defn- jar-path->uri
  {:tag URI}
  [jar-path]
  (-> jar-path
      fs/uri
      (->> (str "jar:"))
      fs/uri))


(defn jar-create-env []
  (doto (HashMap.)
    (.put "create" "true")
    (.put "encoding" "UTF-8")))


(defn make-output-jar-fs
  {:tag FileSystem}
  [{output :jar/output}]
  (ensure-parent! output)
  (-> output
      jar-path->uri
      (fs/file-system (jar-create-env))))

(u/spec-op make-output-jar-fs
           (s/keys :req [:jar/output])
           :jar/file-system)


(defn jar-read-env []
  (doto (HashMap.)
    (.put "encoding" "UTF-8")))


(defn open-jar-fs
  {:tag FileSystem}
  [jar-path]
  (-> jar-path
      jar-path->uri
      (fs/file-system (jar-read-env))))

;;----------------------------------------------------------------------------------------------------------------------
;; Jar
;;----------------------------------------------------------------------------------------------------------------------
(defn- str->input-stream [^String s]
  (-> s .getBytes fs/input-stream))


(defn- add-string! [src dest]
  (let [src (str->input-stream src)]
    (fs/copy! src dest)))


(defn- add-file! [src dest]
  (let [res (fs/copy! src dest)]
    (fs/set-last-modified-time! dest
                                (fs/last-modified-time src))
    res))

;; Clashes
;; adapted from https://github.com/seancorfield/depstar/blob/master/src/hf/depstar/uberjar.clj#L57
(defn clash-strategy [{dest :jar.entry/dest}]
  (cond
    (= "data_readers.clj" (fs/file-name dest))
    :merge-edn

    (re-find #"^META-INF/services/" (str dest))
    :concat-lines

    :else
    :noop))


(defmulti handle-clash clash-strategy)


(defmethod handle-clash :merge-edn
  [{:jar.entry/keys [src dest]
    :as param}]
  (let [current-data-reader (-> dest slurp edn/read-string)
        supplementary-data-reader (-> src slurp edn/read-string)
        content (pr-str (merge current-data-reader
                               supplementary-data-reader))]
    (fs/delete! dest)
    (assoc param
      :jar.adding/result (add-string! content dest)
      :jar.clash/strategy :merge-edn)))


(defmethod handle-clash :concat-lines
  [{:jar.entry/keys [src dest]
    :as param}]
  (let [input (fs/new-input-stream src)
        output (fs/new-ouput-stream dest :append)]
    (assoc param
      :jar.adding/result (fs/copy! input output)
      :jar.clash/strategy :concat-lines)))


(defmethod handle-clash :noop
  [param]
  (assoc param :jar.clash/strategy :noop))

;; TODO: add filter mechanism to remove stuff that shoudn't go in the jar

(defn handle-copy [{src :jar.entry/src
                    dest :jar.entry/dest
                    :as param}]
  (ensure-parent! dest)
  (assoc param
    :jar.adding/result
    (if (string? src)
      (add-string! src dest)
      (add-file! src dest))))


(defn add-entry! [{zfs :jar/file-system
                   :as  param}]
  (let [{dest :jar.entry/dest
         :as param} (update param
                            :jar.entry/dest #(fs/path zfs %))]
    (if (fs/exists? dest)
      (handle-clash param)
      (handle-copy param))))

(u/spec-op add-entry!
           (s/merge :jar/entry
                    (s/keys :req [:jar/file-system])))


(defn add-entries! [{zfs :jar/file-system
                     entries     :jar/entries}]
  (into []
        (comp
          (map #(assoc % :jar/file-system zfs))
          (map add-entry!))
        entries))

(u/spec-op add-entries!
           (s/keys :req [:jar/file-system
                         :jar/entries]))

;;----------------------------------------------------------------------------------------------------------------------
;; Manifest
;;----------------------------------------------------------------------------------------------------------------------
(def meta-dir "META-INF")
(def manifest-name "MANIFEST.MF")
(def manifest-path (fs/path meta-dir manifest-name))


(defn make-manifest-entry [manifest]
  {:jar.entry/src manifest
   :jar.entry/dest manifest-path})

(u/spec-op make-manifest-entry
           :jar/manifest)

;;----------------------------------------------------------------------------------------------------------------------
;; Pom
;;----------------------------------------------------------------------------------------------------------------------
(def maven-dir "maven")


(defn- make-jar-maven-path [group-id artefact-id]
  (fs/path meta-dir maven-dir (str group-id) (str artefact-id) "pom.xml"))


(defn make-pom-entry [{pom :maven/pom
                       group-id :maven/group-id
                       artefact-id :artefact/name}]
  {:jar.entry/src (xml/indent-str pom)
   :jar.entry/dest (make-jar-maven-path group-id artefact-id)})

(u/spec-op make-pom-entry
           (s/keys :req [:maven/pom
                         :maven/group-id
                         :artefact/name]))

;;----------------------------------------------------------------------------------------------------------------------
;; Deps.edn
;;----------------------------------------------------------------------------------------------------------------------
(def deps-dir "deps")

(defn- make-jar-deps-path [group-id artefact-id]
  (fs/path meta-dir deps-dir (str group-id) (str artefact-id) "deps.edn"))

(defn make-deps-entry [{deps :project/deps
                        group-id :maven/group-id
                        artefact-id :artefact/name}]
  {:jar.entry/src (pr-str deps)
   :jar.entry/dest (make-jar-deps-path group-id artefact-id)})

(u/spec-op make-deps-entry
           (s/keys :req [:project/deps
                         :maven/group-id
                         :artefact/name]))
;;----------------------------------------------------------------------------------------------------------------------
;; Pom + manifest
;;----------------------------------------------------------------------------------------------------------------------
(defn make-staples-entries [param]
  [
   (-> param manifest/make-manifest make-manifest-entry)
   (-> param make-pom-entry)
   (-> param make-deps-entry)])

(u/spec-op make-staples-entries
           (s/keys :req[:project/version
                        :project/deps
                        :maven/pom
                        :maven/group-id
                        :artefact/name]
                   :opt [:project/author
                         :jar/main-ns
                         :jar.manifest/overrides]))

;;----------------------------------------------------------------------------------------------------------------------
;; Src files
;;----------------------------------------------------------------------------------------------------------------------
(defn src-dir->jar-entries [dir]
  (->> dir
       fs/walk
       fs/realize
       (into []
             (comp (remove fs/directory?)
                   (map (fn [src-path]
                          {:jar.entry/src src-path
                           :jar.entry/dest (fs/relativize dir src-path)}))))))

(u/spec-op src-dir->jar-entries
           specs/dir-path?
           :jar/entries)

(defn add-src-dir! [{zfs :jar/file-system
                     src-dir :jar/src}]
  (add-entries! {:jar/file-system zfs
                 :jar/entries (src-dir->jar-entries src-dir)}))

;;----------------------------------------------------------------------------------------------------------------------
;; Src jars
;;----------------------------------------------------------------------------------------------------------------------
(defn jar->jar-entries [jar-fs]
  (-> jar-fs
      (fs/path "/")
      fs/walk
      fs/realize
      (->> (into []
                 (comp (remove fs/directory?)
                       (map (fn [src-path]
                              {:jar.entry/src src-path
                               :jar.entry/dest src-path})))))))

(u/spec-op jar->jar-entries
           specs/jar-path?
           :jar/entries)

(defn add-jar! [{zfs :jar/file-system
                 src-jar :jar/src}]
  (with-open [source-zfs (open-jar-fs src-jar)]
    (add-entries! {:jar/file-system zfs
                   :jar/entries (jar->jar-entries source-zfs)})))


(defn- jar? [x] (-> x str (.endsWith ".jar")))

(defn- add-src! [{src :jar/src
                  :as param}]
  (cond
    (sequential? src)   (add-entries! (c-set/rename-keys param {:jar/src :jar/entries}))
    (jar? src)          (add-jar! param)
    (fs/directory? src) (add-src-dir! param)))

(u/spec-op add-src!
           (s/keys :req [:jar/file-system
                         :jar/src])
           :jar/entries)


(defn add-srcs! [{zfs :jar/file-system
                  srcs :jar/srcs}]
  (into []
        (mapcat (fn [src]
                  (add-src! {:jar/file-system zfs
                             :jar/src src})))
        srcs))

(u/spec-op add-srcs!
           (s/keys :req [:jar/file-system
                         :jar/srcs])
           :jar/entries)


(defn- classpath->sources [cp ks]
  (-> cp
      (select-keys ks)
      vals
      (->> (into [] (comp
                      cat
                      (map u/safer-path))))))


(defn simple-jar-srcs [{cp :classpath/index
                        :as param}]

  (into [(make-staples-entries param)]
        (classpath->sources cp #{:classpath/dir})))


(u/spec-op simple-jar-srcs
           (s/keys :req[:classpath/index
                        :project/version
                        :project/deps
                        :maven/pom
                        :maven/group-id
                        :artefact/name]
                   :opt [:project/author
                         :jar/main-ns
                         :jar.manifest/overrides])
           :jar/srcs)


(defn uber-jar-srcs [{cp :classpath/index
                      :as param}]

  (into [(make-staples-entries param)]
        (classpath->sources cp #{:classpath/dir
                                 :classpath/ext-dep
                                 :classpath/jar})))


(u/spec-op uber-jar-srcs
           (s/keys :req[:classpath/index
                        :project/version
                        :project/deps
                        :maven.pom/dir
                        :maven/group-id
                        :artefact/name]
                   :opt [:project/author
                         :jar/main-ns
                         :jar.manifest/overrides])
           :jar/srcs)











(comment
  (clojure.repl/doc fs/walk)
  (fs/delete-if-exists! (u/safer-path "target/mbt.jar"))
  (require '[com.jeremyschoffen.mbt.alpha.building.manifest :as manifest])
  (require '[com.jeremyschoffen.mbt.alpha.building.pom :as pom])
  (require '[com.jeremyschoffen.mbt.alpha.building.deps :as deps])

  (def context (-> {:project/working-dir (u/safer-path ".")
                    :project/version "5.5.5"
                    :artefact/name "mbt"
                    :maven/group-id 'mbt
                    :maven.pom/dir "."}
                   (u/assoc-computed :maven/pom pom/new-pom)
                   (u/assoc-computed :project/deps deps/get-deps)
                   (u/assoc-computed :classpath/index cp/indexed-classpath)
                   (u/assoc-computed :jar/srcs simple-jar-srcs)))

  (fs/directory? (second (:jar/srcs context)))

  (with-open [zfs (make-output-jar-fs {:jar/output (u/safer-path "target/mbt.jar")})]
    (add-srcs! (assoc context :jar/file-system zfs)))



  (with-open [j (open-jar-fs (u/safer-path "/Users/jeremyschoffen/.m2/repository/meander/epsilon/0.0.378/epsilon-0.0.378.jar"))]
    ;(fs/realize (fs/walk (fs/path j "/")))
    (slurp (fs/path j "data_readers.cljc")))

  (with-open [j (open-jar-fs (u/safer-path "/Users/jeremyschoffen/.m2/repository/meander/epsilon/0.0.378/epsilon-0.0.378.jar"))]
    (fs/uri(first (fs/realize (fs/walk (fs/path j "/" "meander"))))))

  (seq (fs/path "toto")))