(ns ^{:author "Jeremy Schoffen"
      :doc "
Default config pertaining to versioning utilities.
      "}
  fr.jeremyschoffen.mbt.alpha.default.config.versioning
  (:require
    [fr.jeremyschoffen.mbt.alpha.default.specs]
    [fr.jeremyschoffen.mbt.alpha.default.config.impl :as impl]
    [fr.jeremyschoffen.mbt.alpha.utils :as u]))


(u/pseudo-nss
  project
  versioning)


(defn tag-base-name
  "Defaults to `project/name` + a suffix depending on `:...mbt.alpha.versioning/major`."
  [{p-name  ::project/name
    major   ::versioning/major}]
  (-> p-name
      (cond-> (and major (not= major :none))
              (str "-" (name major)))))

(u/spec-op tag-base-name
           :param {:req [::project/name]
                   :opt [::versioning/major]}
           :ret ::versioning/tag-base-name)


(def conf {::versioning/major :none
           ::versioning/tag-base-name (impl/calc tag-base-name ::project/name ::versioning/major)})