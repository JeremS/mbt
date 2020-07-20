(ns com.jeremyschoffen.mbt.alpha.default.defaults.gpg
  (:require
    [clojure.java.shell :as shell]
    [com.jeremyschoffen.mbt.alpha.core :as mbt-core]
    [com.jeremyschoffen.mbt.alpha.utils :as u]))

(defn try-fn [f]
  (try
    (f)
    (catch Exception _
      ::error)))

(defn default-gpg-command [& _]
  (or (try-fn #(do (shell/sh "gpg2" "--version") "gpg2"))
      (try-fn #(do (shell/sh "gpg" "--version") "gpg"))))