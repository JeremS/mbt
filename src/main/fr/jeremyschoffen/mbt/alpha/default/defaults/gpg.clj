(ns ^{:author "Jeremy Schoffen"
      :doc "
Default way to use the gpg core apis.
      "}
  fr.jeremyschoffen.mbt.alpha.default.defaults.gpg
  (:require
    [clojure.java.shell :as shell]
    [fr.jeremyschoffen.mbt.alpha.default.specs]))

(defn- try-fn [f]
  (try
    (f)
    (catch Exception _
      nil)))

(defn default-gpg-command
  "Determine which gnupg command can be called if any. Favours gpg2."
  [& _]
  (or (try-fn #(do (shell/sh "gpg2" "--version") "gpg2"))
      (try-fn #(do (shell/sh "gpg" "--version") "gpg"))
      nil))


