(ns com.jeremyschoffen.mbt.alpha.core.gpg-test
  (:require
    [clojure.test :refer [deftest testing]]
    [clojure.spec.test.alpha :as st]
    [clojure.java.shell :as shell]
    [testit.core :refer :all]
    [com.jeremyschoffen.java.nio.alpha.file :as fs]


    [com.jeremyschoffen.mbt.alpha.core.gpg :as gpg]
    [com.jeremyschoffen.mbt.alpha.utils :as u]))

(st/instrument [gpg/gpg-version gpg/sign-file!])

(defn try-fn [f]
  (try
    (f)
    (catch Exception _
      nil)))


(defn make-gpg-command []
  (or (try-fn #(do (shell/sh "gpg2" "--version") "gpg2"))
      (try-fn #(do (shell/sh "gpg" "--version") "gpg"))))

(def gpg-cmd (make-gpg-command))


(def gpg-test-dir (u/safer-path "resources-test" "gpg-tests"))
(def gpg-home-dir (u/safer-path gpg-test-dir "gpg"))
(def file-a (u/safer-path gpg-test-dir "file-a"))
(def file-b (u/safer-path gpg-test-dir "file-b"))

(def conf {:gpg/command  gpg-cmd
           :gpg/home-dir gpg-home-dir
           :gpg/key-id "tester@test.com"
           :gpg/pass-phrase "test"
           :gpg/sign! {:gpg.sign!/in file-a}})

(def out (-> conf
             :gpg/sign!
             gpg/make-sign-out))

(def socket-files
  (mapv (partial u/safer-path gpg-home-dir)
        ["S.gpg-agent"
         "S.gpg-agent.browser"
         "S.gpg-agent.extra"
         "S.gpg-agent.ssh"
         "S.scdaemon"]))

(defn clean! []
  (fs/delete! out)
  (doseq [socket-file socket-files]
    (fs/delete-if-exists! socket-file)))

(deftest signing
  (when gpg-cmd
    (let [res (gpg/sign-file! conf)
          check-a (shell/sh gpg-cmd "--homedir" (str gpg-home-dir) "--verify" (str out))
          check-b (shell/sh gpg-cmd "--homedir" (str gpg-home-dir) "--verify" (str out) (str file-b))]

      (facts
        (-> res :shell/result :exit) => 0
        (:exit check-a) => 0
        (:exit check-b) => 1)

      (clean!))))
