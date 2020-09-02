(ns fr.jeremyschoffen.mbt.alpha.docs.config.data
  (:require
    [clojure.spec.alpha :as s]
    [meander.epsilon :as m]
    [fr.jeremyschoffen.mapiform.alpha.specs.db :as mapi-db]
    [fr.jeremyschoffen.textp.alpha.lib.compilation :as compi]

    [fr.jeremyschoffen.mbt.alpha.core]
    [fr.jeremyschoffen.mbt.alpha.default]
    [fr.jeremyschoffen.mbt.alpha.default.specs]))


(def mbt-ns "fr.jeremyschoffen.mbt.alpha")

(def mbt-docs-ns (str mbt-ns ".docs"))

(defn mbt? [n]
  (clojure.string/starts-with? n mbt-ns))

(defn mbt-docs? [n]
  (clojure.string/starts-with? n mbt-docs-ns))


(defn to-document? [x]
  (let [n (namespace x)]
    (and (mbt? n)
         (not (mbt-docs? n)))))

(defn get-mbt-config-keys []
  (into (sorted-set)
        (m/search (s/registry)
                  {(m/and (m/pred keyword?)
                          (m/pred to-document?)
                          ?spec)
                   ?_}
                  ?spec)))


(defn private? [sym]
  (some-> sym resolve meta :private))


(defn make-spec->constructors []
  (transduce
    (remove (fn [[ _ v]]
              (private? v)))
    (completing (fn [acc [k v]]
                  (update acc k (fnil conj (sorted-set)) v)))
    {}
    (m/search @mapi-db/specs-store
      {?v {:ret ?k}}
      [?k ?v])))


(def config-keys (get-mbt-config-keys))
(def spec->constructors (make-spec->constructors))

(defn get-contructors [config-key]
  (get spec->constructors config-key (sorted-set)))

(defn get-description [config-key]
  #:config-key{:name config-key
               :spec (some-> config-key s/get-spec s/describe)
               :constructors (get-contructors config-key)})

