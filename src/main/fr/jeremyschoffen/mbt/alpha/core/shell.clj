(ns ^{:author "Jeremy Schoffen"
      :doc "
Api wrapping `clojure.java.shell`
      "}
  fr.jeremyschoffen.mbt.alpha.core.shell
  (:require
    [clojure.java.shell :as sh]
    [fr.jeremyschoffen.mbt.alpha.core.specs]
    [fr.jeremyschoffen.mbt.alpha.utils :as u]))


(u/pseudo-nss
  project
  shell)

;; inspired by https://github.com/EwenG/badigeon/blob/master/src/badigeon/exec.clj

(defn- get-english-env
  "Returns env vars as a map with clojure keywords and LANGUAGE set to 'en'"
  []
  (let [env (System/getenv)]
    (assoc (zipmap (map keyword (keys env)) (vals env))
      :LANGUAGE "en")))


(defn safer-sh
  "Wrapper around the `clojure.java.shell/sh function. Position the sh current dir on the
  project's working dir if provided.`"
  [{wd ::project/working-dir
    cmd ::shell/command}]
  (let [env (get-english-env)]
    (sh/with-sh-env env
      (sh/with-sh-dir (or wd (u/safer-path))
        (apply sh/sh cmd)))))

(u/spec-op safer-sh
           :param {:req [::shell/command]
                   :opt [::project/working-dir]})
