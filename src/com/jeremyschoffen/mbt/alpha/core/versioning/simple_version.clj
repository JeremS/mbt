(ns com.jeremyschoffen.mbt.alpha.core.versioning.simple-version
  (:require
    [cognitect.anomalies :as anom]
    [com.jeremyschoffen.mbt.alpha.core.utils :as u]))


(defrecord SimpleVersion [base-number distance sha dirty]
  Object
  (toString [_]
    (-> (str base-number)
        (cond-> (pos? distance) (str "-" distance "-g" sha)
                dirty           (str "-DIRTY")))))


(def initial-simple-version (SimpleVersion. 0 0 "" false))


(defn bump [v]
  (let [{:keys [base-number distance sha dirty]} v]
    (when (zero? distance)
      (throw (ex-info "Duplicating tag."
                      {::anom/category ::anom/forbidden
                       :mbt/error :versioning/duplicating-tag})))
    (SimpleVersion. (+ base-number distance) 0 sha dirty)))
