(ns com.jeremyschoffen.mbt.alpha.core.gpg
  (:require
    [clojure.spec.alpha :as s]
    [com.jeremyschoffen.java.nio.alpha.file :as fs]
    [com.jeremyschoffen.mbt.alpha.core.specs]
    [com.jeremyschoffen.mbt.alpha.core.shell :as shell]
    [com.jeremyschoffen.mbt.alpha.utils :as u])
  (:import
    [java.io StringReader]))


(defn gpg-version [{cmd :gpg/command
                    :as param}]
  (let [res (shell/safer-sh (assoc param
                              :shell/command [cmd "--version"]))
        {:keys [exit out]} res]
    (when (= 0 exit)
      (->> out
           (clojure.string/split-lines)
           first
           (re-matches #".* (?:(\d+)\.(\d+)\.(\d+))")
           rest
           (map #(Integer/parseInt %))
           vec))))

(u/spec-op gpg-version
           :param {:req [:gpg/command]}
           :ret :gpg/version)


(def gpg-v2-1 [2 1 0])
(def gpg-v2 [2 0 0])


(defn- gr-or-eq [x y]
  (let [c (compare x y)]
    (or (zero? c) (pos? c))))


;; Mostly from https://github.com/boot-clj/boot/blob/master/boot/pod/src/boot/gpg.clj
(defn make-sign-cmd [param]
  (let [{cmd :gpg/command
         gpg-v :gpg/version
         home-dir :gpg/home-dir
         gpg-key :gpg/key-id
         gpg-pass-phrase :gpg/pass-phrase
         sign-opts :gpg/sign!} (u/ensure-computed param
                                                  :gpg/command (constantly "gpg")
                                                  :gpg/version gpg-version)
        {p-in :gpg.sign!/in
         p-out :gpg.sign!/out} sign-opts

        home-dir-opt    (when home-dir
                          ["--homedir" (str home-dir)])

        out-opt         (when p-out
                          ["--output" (str p-out)])

        key-opt         (when gpg-key
                          ["--default-key" gpg-key])

        pass-phrase-opt (when gpg-pass-phrase
                          (let [pinentry-opt (when (gr-or-eq gpg-v gpg-v2-1)
                                               ["--pinentry-mode" "loopback"])
                                batch-opt (when (gr-or-eq gpg-v gpg-v2)
                                            ["--batch"])]
                            `[~@pinentry-opt ~@batch-opt "--passphrase-fd" "0"]))

        pass-phrase-in  (when gpg-pass-phrase
                          [:in (StringReader. gpg-pass-phrase)])]
    `[~cmd "--yes" "-ab"
      ~@home-dir-opt
      ~@out-opt
      ~@key-opt
      ~@pass-phrase-opt
      "--" ~(str p-in)
      ~@pass-phrase-in]))

(u/spec-op make-sign-cmd
           :param {:req [:gpg/sign!]
                   :opt [:gpg/command
                         :gpg/version
                         :gpg/home-dir
                         :gpg/key-id
                         :gpg/pass-phrase]})


(defn make-sign-out [{in :gpg.sign!/in}]
  (let [p (fs/parent in)
        n (-> in
              fs/file-name
              (str ".asc"))]
    (if p
      (fs/path p n)
      (fs/path n))))

(u/spec-op make-sign-out
           :param {:req [:gpg.sign!/in]}
           :ret :gpg.sign!/out)


(defn ensure-sign-out [param]
  (update param
          :gpg/sign! u/ensure-computed :gpg.sign!/out make-sign-out))

(u/spec-op ensure-sign-out
           :param {:req [:gpg/sign!]})


(defn sign-file! [param]
  (let [param (ensure-sign-out param)
        spec (:gpg/sign! param)
        cmd (make-sign-cmd param)]
    (assoc spec
      :shell/command cmd
      :shell/result
      (shell/safer-sh (assoc param :shell/command cmd)))))

(u/spec-op sign-file!
           :deps [make-sign-cmd shell/safer-sh]
           :param {:req [:gpg/sign!]
                   :opt [:gpg/command
                         :gpg/home-dir
                         :gpg/key-id
                         :gpg/pass-phrase
                         :gpg/version
                         :project/working-dir]}
           :ret (s/keys :req [:gpg.sign!/in
                              :gpg.sign!/out
                              :shell/command
                              :shell/result]))

