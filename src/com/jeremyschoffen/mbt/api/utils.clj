(ns com.jeremyschoffen.mbt.api.utils
  (:require
    [clojure.spec.alpha :as s]
    [clojure.tools.logging :as log]
    [com.jeremyschoffen.java.nio.file :as fs]))


(defn safer-path
  ([]
   (safer-path "."))
  ([& args]
   (-> (apply fs/path args)
       fs/absolute-path
       fs/normalize)))

(defn wd []
  (safer-path))

;; totally riped from clojure core...
(defn assoc-computed
  ([m k f]
   (assoc m k (f m)))
  ([m k f & kfs]
   (let [ret (assoc-computed m k f)]
     (if kfs
       (if (next kfs)
         (recur ret (first kfs) (second kfs) (nnext kfs))
         (throw (IllegalArgumentException.
                  "assoc-computed expects even number of arguments after map/vector, found odd number")))
       ret))))


(defn merge-computed [m f]
  (merge m (f m)))

;;----------------------------------------------------------------------------------------------------------------------
;; Utils
;;----------------------------------------------------------------------------------------------------------------------
(defmacro ensure-keys [m & kvs]
  (assert (even? (count kvs)))
  (let [res (gensym "res")]
    `(let [~res ~m
           ~@(apply concat (for [[k v] (partition 2 kvs)]
                             `[~res (if (contains? ~res ~k)
                                      ~res
                                      (assoc ~res ~k ~v))]))]
       ~res)))


(defmacro ensure-key [m k v]
  (list `ensure-keys m k v))


(defmacro spec-op
  ([n param-spec]
   (list `spec-op n param-spec nil))
  ([n param-spec ret]
   (let [spec (cond-> [:args (list `s/cat :param param-spec)]
                      ret (conj :ret ret))]
     `(s/fdef ~n ~@spec))))





