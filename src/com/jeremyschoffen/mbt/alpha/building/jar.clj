(ns com.jeremyschoffen.mbt.alpha.building.jar
  (:require
    [clojure.data.xml :as xml]
    [clojure.edn :as edn]
    [clojure.spec.alpha :as s]
    [clojure.string :as string]
    [com.jeremyschoffen.java.nio.file :as fs]
    [com.jeremyschoffen.mbt.alpha.specs :as specs]
    [com.jeremyschoffen.mbt.alpha.utils :as u])
  (:import
    (java.nio.file FileSystem FileSystems)
    (java.net URI)
    (java.util HashMap)))

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
;; Manifest
;;----------------------------------------------------------------------------------------------------------------------
(def meta-dir "META-INF")
(def manifest-name "MANIFEST.MF")
(def manifest-path (fs/path meta-dir manifest-name))


(defn make-manifest-entry [{manifest :jar/manifest}]
  {:jar.entry/src manifest
   :jar.entry/dest manifest-path})

(u/spec-op make-manifest-entry
           (s/keys :req [:jar/manifest]))

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

;;----------------------------------------------------------------------------------------------------------------------
;; Src jars
;;----------------------------------------------------------------------------------------------------------------------
(defn jar->jar-entries [jar-fs]
  (-> jar-fs
      (fs/path "/")
      fs/walk
      fs/realize
      (->> (map (fn [src-path]
                  {:jar.entry/src src-path
                   :jar.entry/dest (-> src-path fs/path)})))))

(u/spec-op jar->jar-entries
           specs/jar-path?
           :jar/entries)


(comment
  (with-open [zfs (open-jar-fs (u/safer-path "target/mbt.jar"))]
    (jar->jar-entries zfs)))

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
                    :as param}
                   zipped-dest]
  (ensure-parent! zipped-dest)
  (assoc param
    :jar.adding/result
    (if (string? src)
      (add-string! src zipped-dest)
      (add-file! src zipped-dest))))

(defn add-jo-jar! [{zfs :jar/file-system
                    dest :jar.entry/dest
                    :as param}]
  (let [dest (fs/path zfs dest)]
    (if (fs/exists? dest)
      (handle-clash param)
      (handle-copy param dest))))

(u/spec-op add-jo-jar!
           (s/merge :jar/entry
                    (s/keys :req [:jar/file-system])))


(defn jar! [{zfs :jar/file-system
             entries :jar/entries}]
  (into []
        (comp
          (map #(assoc % :jar/file-system zfs))
          (map add-jo-jar!))
        entries))

(u/spec-op jar!
           (s/keys :req [:jar/file-system
                         :jar/entries]))







(comment
  (clojure.repl/doc fs/walk)
  (fs/delete-if-exists! (u/safer-path "target/mbt.jar"))
  (require '[com.jeremyschoffen.mbt.alpha.building.manifest :as manifest])
  (require '[com.jeremyschoffen.mbt.alpha.building.pom :as pom])

  (def context {:project/version "5.5.5"
                :artefact/name "mbt"
                :maven/group-id 'mbt
                :maven.pom/dir "."
                :jar/output (u/safer-path "target/mbt.jar")})


  (-> context
      (assoc :jar/entries (conj (src-dir->jar-entries "src")
                            (make-manifest-entry {:jar/manifest (manifest/make-manifest context)})
                            (make-pom-entry (assoc context :maven/pom (pom/new-pom context)))))
      (u/assoc-computed :jar/file-system make-output-jar-fs)
      (as-> ctxt
            (with-open [_ (-> ctxt :jar/file-system fs/file-system)]
              (jar! ctxt))))

  (with-open [zfs (make-output-jar-fs {:jar/output (u/safer-path "target/mbt.jar")})]
    (fs/realize (fs/walk (fs/path zfs "/"))))



  (with-open [j (open-jar-fs (u/safer-path "/Users/jeremyschoffen/.m2/repository/meander/epsilon/0.0.378/epsilon-0.0.378.jar"))]
    (fs/realize (fs/walk (fs/path j "/")))))
    ;(slurp (fs/path j "data_readers.cljc"))))