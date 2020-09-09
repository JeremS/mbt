(ns ^{:author "Jeremy Schoffen"
      :doc "
Default config pertaining to the cleaning utility..
      "}
  fr.jeremyschoffen.mbt.alpha.default.config.cleaning
  (:require
    [fr.jeremyschoffen.mbt.alpha.default.config.impl :as impl]
    [fr.jeremyschoffen.mbt.alpha.default.specs]
    [fr.jeremyschoffen.mbt.alpha.utils :as u]))

(u/pseudo-nss
  cleaning
  project)

(defn cleaning-target
  "Default cleaning dir -> default output-dir."
  [{out ::project/output-dir}]
  out)

(u/spec-op cleaning-target
           :param {:req [::project/output-dir]}
           :ret ::cleaning/target)


(def conf {::cleaning/target (impl/calc cleaning-target ::project/output-dir)})
