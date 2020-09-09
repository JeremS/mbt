(ns ^{:author "Jeremy Schoffen"
      :doc "
Default config pertaining to gpg utilities.
      "}
  fr.jeremyschoffen.mbt.alpha.default.config.gpg
  (:require
    [clojure.spec.alpha :as s]
    [clojure.java.shell :as sh]
    [fr.jeremyschoffen.mbt.alpha.core :as mbt-core]
    [fr.jeremyschoffen.mbt.alpha.default.specs]
    [fr.jeremyschoffen.mbt.alpha.default.config.impl :as impl]
    [fr.jeremyschoffen.mbt.alpha.utils :as u]))

(u/pseudo-nss
  gpg)

(defn- try-fn [f]
  (try
    (f)
    (catch Exception _
      nil)))

(defn gpg-command
  "Default gpg command, \"gpg2\" or \"gpg\" as long as one is present on the system."
  [_]
  (or (try-fn #(do (sh/sh "gpg2" "--version") "gpg2"))
      (try-fn #(do (sh/sh "gpg" "--version") "gpg"))
      nil))

(u/spec-op gpg-command
           :ret (s/nilable ::gpg/command))


(defn gpg-version
  "Look up the gpg version currently installed. See [[fr.jeremyschoffen.mbt.alpha.core/gpg-version]]."
  [param]
  (when-let [command (::gpg/command param)]
    (mbt-core/gpg-version {::gpg/command command})))

(u/spec-op gpg-version
           :param {:opt [::gpg/command]}
           :ret (s/nilable ::gpg/version))


(def conf2 {::gpg/command (impl/calc gpg-command)
            ::gpg/version (impl/calc gpg-version ::gpg/command)})