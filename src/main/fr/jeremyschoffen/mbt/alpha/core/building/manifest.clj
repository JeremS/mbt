(ns ^{:author "Jeremy Schoffen"
      :doc "
Api providing utilities when generating manifest files.
      "}
  fr.jeremyschoffen.mbt.alpha.core.building.manifest
  (:require
    [clojure.spec.alpha :as s]
    [clojure.string :as string]
    [fr.jeremyschoffen.mbt.alpha.core.specs]
    [fr.jeremyschoffen.mbt.alpha.version :as v]
    [fr.jeremyschoffen.mbt.alpha.utils :as u]))

;;----------------------------------------------------------------------------------------------------------------------
;; adapted from https://github.com/EwenG/badigeon/blob/master/src/badigeon/jar.clj
(defn  make-base-manifest [{author :project/author}]
  {"Created-By" (str "Mbt v" v/version)
   "Built-By"   (or author (System/getProperty "user.name"))
   "Build-Jdk"  (System/getProperty "java.version")})

(u/spec-op make-base-manifest
           :param {:opt [:project/author]}
           :ret (s/map-of string? string?))

(defn- place-sections-last
  "Places sections at the end of the manifest seq, as specified by the
  Manifest spec. Retains ordering otherwise (if mf is ordered)."
  [mf]
  (sort-by val (fn [v1 v2]
                 (and (not (coll? v1)) (coll? v2)))
           (seq mf)))

(declare ^:private format-manifest-entry)

(defn- format-manifest-entry-section [k v]
  (->> (map format-manifest-entry v)
       (cons (str "\nName: " (name k) "\n"))
       (string/join)))

(defn- format-manifest-entry [[k v]]
  (if (coll? v)
    (format-manifest-entry-section k v)
    (->> (str (name k) ": " v)
         (partition-all 70)  ;; Manifest spec says lines <= 72 chars
         (map (partial apply str))
         (string/join "\n ")  ;; Manifest spec says join with "\n "
         (format "%s\n"))))

(defn ^String make-manifest
  "Format the content of a MANIFEST.MF file as a string.

  Several keys option are specified by the keys:
  - `:jar/main-ns`: A namespace to be added to the \"Main\" entry to the manifest. Default to nil.
  - `:jar.manifest/overrides`: A map of additional entries to the manifest. Values of the manifest map can be maps to represent
   manifest sections. By default, the manifest contains the \"Created-by\", \"Built-By\" and \"Build-Jdk\" entries."
  [{main :jar/main-ns
    manifest-overrides :jar.manifest/overrides
    :as param}]
  (let [base-manifest (make-base-manifest param)
        manifest (if main
                   (assoc base-manifest "Main-Class" (munge (str main)))
                   base-manifest)]
    (->> (merge manifest manifest-overrides)
         place-sections-last
         (map format-manifest-entry)
         (cons "Manifest-Version: 1.0\n")
         (string/join ""))))

(u/spec-op make-manifest
           :deps [make-base-manifest]
           :param {:opt [:project/author
                         :jar/main-ns
                         :jar.manifest/overrides]}
           :ret :jar/manifest)
