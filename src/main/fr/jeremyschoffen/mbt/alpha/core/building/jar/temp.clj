(ns ^{:author "Jeremy Schoffen"
      :doc "
Api providing facilities to copy files from different sources into a unique directory.
This directory is intended to be zipped into a jar file.

The function [[fr.jeremyschoffen.mbt.alpha.core.building.jar.temp/add-srcs!]] is the entry point when
copying sources to the temp directory.
      "}
  fr.jeremyschoffen.mbt.alpha.core.building.jar.temp
  (:require
    [clojure.edn :as edn]
    [fr.jeremyschoffen.java.nio.alpha.file :as fs]
    [fr.jeremyschoffen.mbt.alpha.core.building.jar.protocols :as p]
    [fr.jeremyschoffen.mbt.alpha.core.specs]
    [fr.jeremyschoffen.mbt.alpha.utils :as u]))


;;----------------------------------------------------------------------------------------------------------------------
;; Nitty-gritty of copying files to temp dir
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


;;----------------------------------------------------------------------------------------------------------------------
;; Clashes
;;----------------------------------------------------------------------------------------------------------------------
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


;;----------------------------------------------------------------------------------------------------------------------
;; Copying api
;;----------------------------------------------------------------------------------------------------------------------
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


(defn add-entry!
  "Function that copies a jar entry into the temp output, handling the exclusion cases and the clashes."
  [{output   :jar/temp-output
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


(defn add-entries!
  "Use [[fr.jeremyschoffen.mbt.alpha.core.building.jar.temp/add-entry!]] to copy all the entries
  under the key `:jar/entries`"
  [{entries :jar/entries
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


(defn add-srcs!
  "Copy the files grouped under the key `:jar/srcs` into the temp jar directory specified under the key
  `:jar/temp-output`.

  A source can be is anything that satifies [[fr.jeremyschoffen.mbt.alpha.core.building.jar.protocols/JarSource]]

  An optional exclusion function can be passed under the key `:jar/exclude`. If this function returns true, the jar
  entry will be excluded from the jar. It must take only one argument which will be a map with the following keys:
    - `:jar.entry/src`: absolute path to the entry
    - `:jar.entry/dest`: relative path (to the dest jar) indicating where to place the entry in the jar
    - `:jar/temp-output`: path to the temporary directory

  The return value is a sequence of map, each one basically a `jar/entry` with additional keys:
    - `:jar/temp-output` a reminder of the temp output path
    - `:jar.adding/result`: the `:filtered-out` keyword in the case of an exclusion, whichever value was returned by
       whichever copying function was used otherwise.
    - `:jar.clash/strategy`: a keyword indicating which clash strategy has been used for this entry if a clash happened
  "
  [{srcs :jar/srcs
    :as param}]
  (into []
        (comp (map p/to-entries)
              (mapcat #(p/add! % param)))
        srcs))

(u/spec-op add-srcs!
           :param {:req [:jar/temp-output :jar/srcs]
                   :opt [:jar/exclude?]}
           :ret :jar/entries)