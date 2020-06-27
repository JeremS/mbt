(ns com.jeremyschoffen.mbt.alpha.core.building.jar
  (:require
    [clojure.data.xml :as xml]
    [clojure.edn :as edn]
    [clojure.set :as c-set]
    [com.jeremyschoffen.java.nio.file :as fs]
    [com.jeremyschoffen.mbt.alpha.core.building.manifest :as manifest]
    [com.jeremyschoffen.mbt.alpha.core.specs :as specs]
    [com.jeremyschoffen.mbt.alpha.utils :as u])
  (:import
    (java.nio.file FileSystem)
    (java.net URI)
    (java.util HashMap)))


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


(defn- jar-create-env []
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


(defn- jar-read-env []
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
;; Copying files to temp dir
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
(defn- meta-inf-services? [{dest :jar.entry/dest
                            temp-out :jar/temp-output}]
  (->> dest
       (fs/relativize temp-out)
       str
       (re-find #"^META-INF/services/")))

(u/spec-op meta-inf-services?
           :param {:req [:jar.entry/dest
                         :jar/temp-output]})


(defn- clash-strategy [{dest :jar.entry/dest
                        :as param}]
  (cond
    (= "data_readers.cljc" (-> dest fs/file-name str))
    :merge-edn

    (meta-inf-services? param)
    :concat-lines

    :else
    :noop))

(u/spec-op clash-strategy
           :deps [meta-inf-services?]
           :param {:req [:jar/temp-output :jar.entry/dest]})


(defmulti ^:private handle-clash! clash-strategy)

(u/spec-op handle-clash!
           :deps [clash-strategy add-string! add-file!]
           :param {:req [:jar.entry/src
                         :jar.entry/dest
                         :jar/temp-output]}
           :ret :jar/entry)


(defmethod handle-clash! :merge-edn
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


(defmethod handle-clash! :concat-lines
  [{:jar.entry/keys [src dest]
    :as param}]
  (let [input (->> src
                   fs/read-all-lines
                   (cons ""))]
    (assoc param
      :jar.adding/result (fs/write dest input {:open-opts [:append]})
      :jar.clash/strategy :concat-lines)))


(defmethod handle-clash! :noop
  [param]
  (assoc param :jar.clash/strategy :noop))


(defn- handle-copy! [{src :jar.entry/src
                      dest :jar.entry/dest
                      :as  param}]
  (u/ensure-parent! dest)
  (assoc param
    :jar.adding/result
    (if (string? src)
      (add-string! src dest)
      (add-file! src dest))))

(u/spec-op handle-copy!
           :deps [add-string! add-file!]
           :param {:req [:jar.entry/src :jar.entry/dest]}
           :ret :jar/entry)


(defn- add-entry! [{output   :jar/temp-output
                    exclude? :jar/exclude?
                    entry :jar/entry}]
  (let [entry (-> entry
                  (assoc :jar/temp-output output)
                  (update :jar.entry/dest #(fs/resolve output %)))]
    (cond
      (and exclude? (exclude? entry))
      (assoc entry :jar.adding/result :filtered-out)

      (fs/exists? (:jar.entry/dest entry))
      (handle-clash! entry)

      :else
      (handle-copy! entry))))

(u/spec-op add-entry!
           :deps [handle-copy! handle-clash!]
           :param {:req [:jar/entry
                         :jar/temp-output]
                   :opt [:jar/exclude?]})


(defn- add-entries! [{entries :jar/entries
                      :as param}]
  (into []
        (comp
          (map #(assoc param :jar/entry %))
          (map add-entry!))
        entries))

(u/spec-op add-entries!
           :param {:req [:jar/entries
                         :jar/temp-output]
                   :opt [:jar/exclude?]})


;;----------------------------------------------------------------------------------------------------------------------
;; Jar srcs (-> jar entries) -> copy to temp jar dir
;;----------------------------------------------------------------------------------------------------------------------

;; src dir -> jar entries -> ...
(defn- src-dir->jar-entries [dir]
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


(defn- add-src-dir! [{src-dir :jar/src
                      :as param}]
  (-> param
      (assoc :jar/entries (src-dir->jar-entries src-dir))
      add-entries!))


(u/spec-op add-src-dir!
           :param {:req [:jar/src :jar/temp-output]
                   :opt [:jar/exclude?]})


;; src jar -> jar entries -> ...
(defn- jar->jar-entries [jar-fs]
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


(defn- add-jar! [{src-jar :jar/src
                  :as param}]
  (with-open [source-zfs (open-jar-fs src-jar)]
    (-> param
        (assoc :jar/entries (jar->jar-entries source-zfs))
        add-entries!)))


(u/spec-op add-jar!
           :param {:req [:jar/src :jar/temp-output]
                   :opt [:jar/exclude?]})


(defn- add-src! [{src :jar/src
                  :as param}]
  (cond
    (sequential? src)     (add-entries! (c-set/rename-keys param {:jar/src :jar/entries}))
    (fs/directory? src)   (add-src-dir! param)
    (specs/jar-path? src) (add-jar! param)))

(u/spec-op add-src!
           :param {:req [:jar/temp-output :jar/src]
                   :opt [:jar/exclude?]}
           :ret :jar/entries)


(defn add-srcs!
  "Copies the files grouped under the key `:jar/srcs` into
  the temp jar directory."
  [{srcs :jar/srcs
    :as param}]
  (into []
        (mapcat (fn [src]
                  (add-src! (assoc param :jar/src src))))
        srcs))

(u/spec-op add-srcs!
           :param {:req [:jar/temp-output :jar/srcs]
                   :opt [:jar/exclude?]}
           :ret :jar/entries)

;;----------------------------------------------------------------------------------------------------------------------
;; Zipping the temp jar dir into the jar archive
;;----------------------------------------------------------------------------------------------------------------------
(defn make-jar-archive!
  "Zips the a dir into a .jar archive file."
  [{temp :jar/temp-output
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
