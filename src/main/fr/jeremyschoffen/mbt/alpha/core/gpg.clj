(ns ^{:author "Jeremy Schoffen"
      :doc "
Api providing gpg utilities.
      "}
  fr.jeremyschoffen.mbt.alpha.core.gpg
  (:require
    [clojure.spec.alpha :as s]
    [fr.jeremyschoffen.java.nio.alpha.file :as fs]
    [fr.jeremyschoffen.mbt.alpha.core.specs]
    [fr.jeremyschoffen.mbt.alpha.core.shell :as clojure-shell]
    [fr.jeremyschoffen.mbt.alpha.utils :as u])
  (:import
    [java.io StringReader]))


(u/pseudo-nss
  gpg
  gpg.sign!
  project
  shell)

(defn gpg-version
  "Try to get the current gpg version installed on the system. Returns in a vector of 3 ints: major minor and patch
  numbers."
  [{cmd ::gpg/command
    :as param}]
  (let [res (clojure-shell/safer-sh (assoc param
                                      ::shell/command [cmd "--version"]))
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
           :param {:req [::gpg/command]}
           :ret ::gpg/version)


(def ^:private gpg-v2-1 [2 1 0])
(def ^:private gpg-v2 [2 0 0])


(defn- gr-or-eq [x y]
  (let [c (compare x y)]
    (or (zero? c) (pos? c))))


;; Mostly from https://github.com/boot-clj/boot/blob/master/boot/pod/src/boot/gpg.clj
(defn- make-sign-cmd [param]
  (let [{cmd ::gpg/command
         gpg-v ::gpg/version
         home-dir ::gpg/home-dir
         gpg-key ::gpg/key-id
         gpg-pass-phrase ::gpg/pass-phrase
         sign-opts ::gpg/sign!} (u/ensure-computed param
                                                   ::gpg/command (constantly "gpg")
                                                   ::gpg/version gpg-version)
        {p-in ::gpg.sign!/in
         p-out ::gpg.sign!/out} sign-opts

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
           :param {:req [::gpg/sign!]
                   :opt [::gpg/command
                         ::gpg/version
                         ::gpg/home-dir
                         ::gpg/key-id
                         ::gpg/pass-phrase]})


(defn make-sign-out
  "Generates an ouput path for a signature given the path of the file to sign."
  [{in ::gpg.sign!/in}]
  (let [p (fs/parent in)
        n (-> in
              fs/file-name
              (str ".asc"))]
    (if p
      (fs/path p n)
      (fs/path n))))

(u/spec-op make-sign-out
           :param {:req [::gpg.sign!/in]}
           :ret ::gpg.sign!/out)


(defn- ensure-sign-out [param]
  (update param
          ::gpg/sign! u/ensure-computed ::gpg.sign!/out make-sign-out))

(u/spec-op ensure-sign-out
           :param {:req [::gpg/sign!]})


(defn sign-file!
  "Use gnupg to sign a file. The different options are:
  - `:fr...mbt.alpha.gpg/sign!`: in/output passed to the gnupg call
  - `:fr...mbt.alpha.gpg/command`: actual gnupg command to call gpg, gpg2...
  - `:fr...mbt.alpha.gpg/home-dir`: option telling gnupg where the directory containing keys is located
  - `:fr...mbt.alpha.gpg/key-id`: id of the key used to create the signature
  - `:fr...mbt.alpha.gpg/pass-phrase`: option to directly give the pass phrase instead of being prompted by the gpg
    agent during execution
  - `:fr...mbt.alpha.gpg/version`: optional gnupg version in the vector form described in `gpg-version`.
    Used to determine which gpg options may be necessary to use.
  - `:fr...mbt.alpha.project/working-dir`: the project's working dir. It will position the shell used to call gnupg on it.
  "
  [param]
  (let [param (ensure-sign-out param)
        spec (::gpg/sign! param)
        cmd (make-sign-cmd param)]
    (assoc spec
      ::shell/command cmd
      ::shell/result
      (clojure-shell/safer-sh (assoc param ::shell/command cmd)))))

(u/spec-op sign-file!
           :deps [make-sign-cmd shell/safer-sh]
           :param {:req [::gpg/sign!]
                   :opt [::gpg/command
                         ::gpg/home-dir
                         ::gpg/key-id
                         ::gpg/pass-phrase
                         ::gpg/version
                         ::project/working-dir]}
           :ret (s/keys :req [::gpg.sign!/in
                              ::gpg.sign!/out
                              ::shell/command
                              ::shell/result]))
