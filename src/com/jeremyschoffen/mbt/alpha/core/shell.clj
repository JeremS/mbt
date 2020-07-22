(ns com.jeremyschoffen.mbt.alpha.core.shell
  (:require
    [clojure.java.shell :as shell]
    [com.jeremyschoffen.mbt.alpha.core.specs]
    [com.jeremyschoffen.mbt.alpha.utils :as u]))

;; inspired by https://github.com/EwenG/badigeon/blob/master/src/badigeon/exec.clj

(defn- get-english-env
  "Returns env vars as a map with clojure keywords and LANGUAGE set to 'en'"
  []
  (let [env (System/getenv)]
    (assoc (zipmap (map keyword (keys env)) (vals env))
      :LANGUAGE "en")))


(defn safer-sh
  "Wrapper aroungd the `clojure.java.shell/sh function. Position the sh current dir on the
  project's working dir if provided.`"
  [{wd :project/working-dir
    cmd :shell/command}]
  (shell/with-sh-env (get-english-env)
    (shell/with-sh-dir (or wd (u/safer-path))
      (apply shell/sh cmd))))

(u/spec-op safer-sh
           :param {:req [:shell/command]
                   :opt [:project/working-dir]})
