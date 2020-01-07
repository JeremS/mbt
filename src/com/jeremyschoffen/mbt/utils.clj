(ns com.jeremyschoffen.mbt.utils
  (:refer-clojure :exclude [derive])
  (:require
    [clojure.tools.deps.alpha :as deps]
    [me.raynes.fs :as fs]
    [metav.utils :as mu])
  (:import
    [java.nio.file FileVisitResult]))

;;----------------------------------------------------------------------------------------------------------------------
;; Imports from metav's utils
;;----------------------------------------------------------------------------------------------------------------------
(def pwd mu/pwd)
(def assoc-computed mu/assoc-computed)
(def check-spec mu/check-spec)
(def ancestor? mu/ancestor?)

(def merge-defaults mu/merge-defaults)
(def merge&validate mu/merge&validate)

(defmacro check
  ([x]
   `(mu/check ~x))
  ([x msg]
   `(mu/check ~x ~msg)))


(defmacro ensure-keys [m & kvs]
  (list*  `mu/ensure-keys m kvs))

(defmacro ensure-key [m k v]
  (list `mu/ensure-key m k v))


;;----------------------------------------------------------------------------------------------------------------------
;; FS utils
;;----------------------------------------------------------------------------------------------------------------------
(defn strict-ancestor? [path possible-descendant]
  (and (ancestor? path possible-descendant)
       (not= path possible-descendant)))


(defn normalize-in-context [context path]
  (fs/with-cwd (:metav/working-dir context)
    (fs/normalized (fs/file path))))


(defn default-preVisitDirectory [dir attrs]
  FileVisitResult/CONTINUE)


(defn default-postVisitDirectory [dir exception]
  (if (nil? exception)
    FileVisitResult/CONTINUE
    (throw exception)))


(defn default-visitFileFailed [file exception]
  (throw exception))


;;----------------------------------------------------------------------------------------------------------------------
;; Utils
;;----------------------------------------------------------------------------------------------------------------------
(defn map-kv [k-fn v-fn m]
  (persistent!
    (reduce-kv (fn [m k v]
                 (assoc! m (k-fn k) (v-fn v)))
               (transient {})
               m)))

(defn map-keys [k-fn m]
  (map-kv  k-fn identity m))


(defn map-vals [v-fn m]
  (map-kv identity v-fn m))



