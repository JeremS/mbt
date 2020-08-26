(ns fr.jeremyschoffen.mbt.alpha.core.gpg-test
  (:require
    [clojure.test :refer :all]
    [clojure.spec.test.alpha :as st]
    [clojure.java.shell :as clojure-shell]
    [testit.core :refer :all]
    [fr.jeremyschoffen.java.nio.alpha.file :as fs]


    [fr.jeremyschoffen.mbt.alpha.core.gpg :as core-gpg]
    [fr.jeremyschoffen.mbt.alpha.utils :as u]))

(u/mbt-alpha-pseudo-nss
  gpg
  gpg.sign!
  shell)


(st/instrument [core-gpg/gpg-version core-gpg/sign-file!])

(defn try-fn [f]
  (try
    (f)
    (catch Exception _
      nil)))


(defn make-gpg-command []
  (or (try-fn #(do (clojure-shell/sh "gpg2" "--version") "gpg2"))
      (try-fn #(do (clojure-shell/sh "gpg" "--version") "gpg"))))

(def gpg-cmd (make-gpg-command))


(def gpg-test-dir (u/safer-path "resources-test" "gpg-tests"))
(def gpg-home-dir (u/safer-path gpg-test-dir "gpg"))
(def file-a (u/safer-path gpg-test-dir "file-a"))
(def file-b (u/safer-path gpg-test-dir "file-b"))

(def conf {::gpg/command  gpg-cmd
           ::gpg/home-dir gpg-home-dir
           ::gpg/key-id "tester@test.com"
           ::gpg/pass-phrase "test"
           ::gpg/sign! {::gpg.sign!/in file-a}})

(def out (-> conf
             ::gpg/sign!
             core-gpg/make-sign-out))

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
  (if gpg-cmd
    (let [res (core-gpg/sign-file! conf)
          check-a (clojure-shell/sh gpg-cmd "--homedir" (str gpg-home-dir) "--verify" (str out))
          check-b (clojure-shell/sh gpg-cmd "--homedir" (str gpg-home-dir) "--verify" (str out) (str file-b))]

      (facts
        (-> res ::shell/result :exit) => 0
        (:exit check-a) => 0
        (:exit check-b) => 1)

      (clean!))

    (is false
        "gpg not found")))
