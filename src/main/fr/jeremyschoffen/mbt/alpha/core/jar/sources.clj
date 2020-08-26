(ns ^{:author "Jeremy Schoffen"
      :doc "
Default implementations of the protocols found in [[fr.jeremyschoffen.mbt.alpha.core.jar.protocols]].

Types implementing [[fr.jeremyschoffen.mbt.alpha.core.jar.protocols/JarSource]]:
- `clojure.lang.Sequential`: sequences of `:jar/entry` producing itself as a
  [[fr.jeremyschoffen.mbt.alpha.core.building.jar.protocols/JarEntries]].
- `java.nio.file.Path`: path to a directory or a jar. A path to a directory will produce a
  [[fr.jeremyschoffen.mbt.alpha.core.jar.sources/SourceDir]] record. A path to a jar will
  produce a [[fr.jeremyschoffen.mbt.alpha.core.jar.sources/SourceDir]] record. Both record types
  implement [[fr.jeremyschoffen.mbt.alpha.core.jar.protocols/JarEntries]].
      "}
  fr.jeremyschoffen.mbt.alpha.core.jar.sources
  (:require
    [clojure.spec.alpha :as s]
    [cognitect.anomalies :as anom]
    [fr.jeremyschoffen.java.nio.alpha.file :as fs]

    [fr.jeremyschoffen.mbt.alpha.core.jar.fs :as jar-fs]
    [fr.jeremyschoffen.mbt.alpha.core.jar.protocols :as p]
    [fr.jeremyschoffen.mbt.alpha.core.jar.temp :as temp]
    [fr.jeremyschoffen.mbt.alpha.core.specs :as specs]
    [fr.jeremyschoffen.mbt.alpha.utils :as u])
  (:import (java.nio.file Path)
           (clojure.lang Sequential)))

(u/mbt-alpha-pseudo-nss
  jar
  jar.entry)


(defn- throw-not-src [x]
  (throw (ex-info (format "Can't turn %s into a jar entries" x)
                  {::anom/category ::anom/unsupported
                   :this x})))


(extend-type Object
  p/JarSource
  (to-entries
    ([this]   (throw-not-src this))
    ([this _] (throw-not-src this)))

  p/JarEntries
  (add! [this]
    (throw (ex-info (format "Can't add %s to a jar." this)
                    {::anom/category ::anom/unsupported
                     :this this}))))


(defn add-exclusion
  "Add an exclusion to a context under the key `:jar/exclude`.
  If the context doesn't have one it just associates the function to the map. If an exclusion function
  is already there it joins the two using [[clojure.core/some-fn]]."
  [conf exclude]
  (let [e (::jar/exclude? conf)
        exclude' (cond-> exclude
                         e (some-fn e))]
    (assoc conf ::jar/exclude? exclude')))


;;----------------------------------------------------------------------------------------------------------------------
;; Sequence of :jar-entry
;;----------------------------------------------------------------------------------------------------------------------
(defn- add-sequential-entries! [conf entries]
  (-> conf
      (assoc ::jar/entries entries)
      temp/add-entries!))


(extend-type Sequential
  p/JarEntries
  (add! [this conf]
    (add-sequential-entries! conf this))

  p/JarSource
  (to-entries
    ([this] this)
    ([this exclude]
     (with-meta this
                {`p/add! (fn [this conf]
                           (-> conf
                               (add-exclusion exclude)
                               (add-sequential-entries! this)))}))))


;;----------------------------------------------------------------------------------------------------------------------
;; Source directory entries
;;----------------------------------------------------------------------------------------------------------------------
(defn- src-dir->jar-entries [dir]
  (->> dir
       fs/walk
       fs/realize
       (into []
             (comp (remove fs/directory?)
                   (map (fn [src-path]
                          {::jar.entry/src src-path
                           ::jar.entry/dest (fs/relativize dir src-path)}))))))

(u/simple-fdef src-dir->jar-entries
               specs/dir-path?
               ::jar/entries)


(defn- add-src-dir! [conf src-dir]
  (-> conf
      (assoc ::jar/entries (src-dir->jar-entries src-dir))
      temp/add-entries!))

(s/fdef add-src-dir!
        :args (s/cat :conf (s/keys :req [::jar/temp-output]
                                   :opt [::jar/exclude?])
                     :src specs/dir-path?))


(defrecord SourceDir [path exclude]
  p/JarSource
  (to-entries [this] this)
  (to-entries [_ exclude']
    (SourceDir. path exclude'))

  p/JarEntries
  (add! [_ conf]
    (let [conf (if exclude
                 (add-exclusion conf exclude)
                 conf)]
      (add-src-dir! conf path))))


;;----------------------------------------------------------------------------------------------------------------------
;; Source jar
;;----------------------------------------------------------------------------------------------------------------------
(defn- jar->jar-entries [jar-fs]
  (-> jar-fs
      (fs/path "/")
      fs/walk
      fs/realize
      (->> (into []
                 (comp (remove fs/directory?)
                       (map (fn [src-path]
                              {::jar.entry/src src-path
                               ::jar.entry/dest (->> src-path
                                                     (map str)
                                                     (apply fs/path))})))))))

(u/simple-fdef jar->jar-entries
               specs/file-system?
               ::jar/entries)


(defn- add-jar! [conf src-jar]
  (with-open [source-zfs (jar-fs/read-only-jar-fs src-jar)]
    (-> conf
        (assoc ::jar/entries (jar->jar-entries source-zfs))
        temp/add-entries!)))

(s/fdef add-jar!
        :args (s/cat :conf (s/keys :req [::jar/temp-output]
                                   :opt [::jar/exclude?])
                     :src specs/jar-path?))


(defrecord SourceJar [path exclude]
  p/JarSource
  (to-entries [this] this)
  (to-entries [_ exclude']
    (SourceJar. path exclude'))

  p/JarEntries
  (add! [_ conf]
    (let [conf (if exclude
                 (add-exclusion conf exclude)
                 conf)]
      (add-jar! conf path))))


;;----------------------------------------------------------------------------------------------------------------------
;; path -> entries
;;----------------------------------------------------------------------------------------------------------------------
(defn- path->entries [{path :path :as m}]
  (cond
    (specs/dir-path? path) (map->SourceDir m)
    (specs/jar-path? path) (map->SourceJar m)
    :else (throw (ex-info (format "Can't turn %s into jar entries" path)
                          {::anom/category ::anom/unsupported
                           :mbt/error ::unsuported-file-type}))))


(extend-protocol p/JarSource
  Path
  (to-entries
    ([this]
     (path->entries {:path this}))
    ([this exclude]
     (path->entries {:path this
                     :exclude exclude}))))
