(ns com.jeremyschoffen.mbt.alpha.core.building.jar
  (:require
    [clojure.data.xml :as xml]
    [clojure.edn :as edn]
    [clojure.set :as c-set]
    [com.jeremyschoffen.java.nio.file :as fs]
    [com.jeremyschoffen.mbt.alpha.core.building.manifest :as manifest]
    [com.jeremyschoffen.mbt.alpha.core.specs :as specs]
    [com.jeremyschoffen.mbt.alpha.core.utils :as u])
  (:import
    (java.nio.file FileSystem)
    (java.net URI)
    (java.util HashMap)))

;; TODO: decide what to do with wayward files on a classpath.
;; TODO: implement a way to filter jar entries.
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
  (u/ensure-parent! output)
  (-> output
      jar-path->uri
      (fs/file-system (jar-create-env))))

(u/spec-op make-output-jar-fs
           :param {:req [:jar/output]}
           :ret :jar/file-system)

(defn jar-read-env []
  (doto (HashMap.)
    (.put "encoding" "UTF-8")))


(defn open-jar-fs
  {:tag FileSystem}
  [jar-path]
  (-> jar-path
      jar-path->uri
      (fs/file-system (jar-read-env))))

(u/simple-fdef open-jar-fs
               specs/jar-path?
               :jar/file-system)
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
    (= "data_readers.cljc" (-> dest fs/file-name str))
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
      :content content
      :jar.adding/result  (add-string! content dest)
      :jar.clash/strategy :merge-edn)))

;; TODO: rework that, the present impl doesn't insert a line break between the 2 file contents.
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
  (u/ensure-parent! dest)
  (assoc param
    :jar.adding/result
    (if (string? src)
      (add-string! src dest)
      (add-file! src dest))))


(defn add-entry! [{output :jar/temp-output
                   :as  param}]
  (let [{dest :jar.entry/dest
         :as param} (update param
                            :jar.entry/dest #(fs/resolve output %))]
    (if (fs/exists? dest)
      (handle-clash param)
      (handle-copy param))))

(u/spec-op add-entry!
           :param {:req [:jar/temp-output :jar.entry/src :jar.entry/dest]})


(defn add-entries! [{entries :jar/entries
                     output :jar/temp-output}]
  (into []
        (comp
          (map #(assoc % :jar/temp-output output))
          (map add-entry!))
        entries))

(u/spec-op add-entries!
           :param {:req [:jar/entries :jar/temp-output]})

;;----------------------------------------------------------------------------------------------------------------------
;; Manifest
;;----------------------------------------------------------------------------------------------------------------------
(def meta-dir "META-INF")
(def manifest-name "MANIFEST.MF")
(def manifest-path (fs/path meta-dir manifest-name))

;;TODO: For uniformity, consider using a map parameter
(defn make-manifest-entry [manifest]
  {:jar.entry/src manifest
   :jar.entry/dest manifest-path})

(u/simple-fdef make-manifest-entry
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
           :param {:req [:maven/pom
                         :maven/group-id
                         :artefact/name]})

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
           :param {:req [:project/deps
                         :maven/group-id
                         :artefact/name]})
;;----------------------------------------------------------------------------------------------------------------------
;; Pom + manifest
;;----------------------------------------------------------------------------------------------------------------------
(defn make-staples-entries [param]
  [(-> param manifest/make-manifest make-manifest-entry)
   (-> param make-pom-entry)
   (-> param make-deps-entry)])

(u/spec-op make-staples-entries
           :deps [manifest/make-manifest make-manifest-entry make-pom-entry make-deps-entry]
           :param {:req[:project/deps
                        :maven/pom
                        :maven/group-id
                        :artefact/name]
                   :opt [:project/author
                         :jar/main-ns
                         :jar.manifest/overrides]})

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

(u/simple-fdef src-dir->jar-entries
               specs/dir-path?
               :jar/entries)


(defn add-src-dir! [{out :jar/temp-output
                     src-dir :jar/src}]
  (add-entries! {:jar/temp-output  out
                 :jar/entries (src-dir->jar-entries src-dir)}))

(u/spec-op add-src-dir!
           :param {:req [:jar/src :jar/temp-output]})

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
                               :jar.entry/dest (->> src-path
                                                    (map str)
                                                    (apply fs/path))})))))))

(u/simple-fdef jar->jar-entries
               specs/file-system?
               :jar/entries)


(defn add-jar! [{out     :jar/temp-output
                 src-jar :jar/src}]
  (with-open [source-zfs (open-jar-fs src-jar)]
    (add-entries! {:jar/temp-output out
                   :jar/entries (jar->jar-entries source-zfs)})))

(u/spec-op add-jar!
           :param {:req [:jar/src :jar/temp-output]})


(defn- add-src! [{src :jar/src
                  :as param}]
  (cond
    (sequential? src)     (add-entries! (c-set/rename-keys param {:jar/src :jar/entries}))
    (fs/directory? src)   (add-src-dir! param)
    (specs/jar-path? src) (add-jar! param)))



(u/spec-op add-src!
           :param {:req [:jar/temp-output :jar/src]}
           :ret :jar/entries)


(defn add-srcs!
  "Copies the files grouped under the key `:jar/srcs` into
  the temp jar directory."
  [{out  :jar/temp-output
    srcs :jar/srcs}]
  (into []
        (mapcat (fn [src]
                  (add-src! {:jar/temp-output out
                             :jar/src         src})))
        srcs))

(u/spec-op add-srcs!
           :param {:req [:jar/temp-output :jar/srcs]}
           :ret :jar/entries)


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
           :param {:req [:classpath/index
                         :project/deps
                         :maven/pom
                         :maven/group-id
                         :artefact/name]
                   :opt [:project/author
                         :jar/main-ns
                         :jar.manifest/overrides]}
           :ret :jar/srcs)


(defn uber-jar-srcs [{cp :classpath/index
                      :as param}]

  (into [(make-staples-entries param)]
        (classpath->sources cp #{:classpath/dir
                                 :classpath/ext-dep
                                 :classpath/jar})))

(u/spec-op uber-jar-srcs
           :param {:req[:classpath/index
                        :project/deps
                        :maven/pom
                        :maven/group-id
                        :artefact/name]
                   :opt [:project/author
                         :jar/main-ns
                         :jar.manifest/overrides]}
           :ret :jar/srcs)


(defn make-jar-archive! [{temp :jar/temp-output
                          output :jar/output
                          :as               param}]
  (with-open [zfs (make-output-jar-fs param)]
    (doseq [src (->> temp
                     fs/walk
                     fs/realize
                     (remove fs/directory?))]
      (let [dest (->> src
                      (fs/relativize temp)
                      (fs/path zfs))]
        (u/ensure-parent! dest)
        (fs/copy! src dest))))
  output)

(u/spec-op make-jar-archive!
           :deps [make-output-jar-fs]
           :param {:req [:jar/temp-output :jar/output]})
