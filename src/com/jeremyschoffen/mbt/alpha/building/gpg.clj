(ns com.jeremyschoffen.mbt.alpha.building.gpg
  (:require
    [com.jeremyschoffen.mbt.alpha.specs]
    [com.jeremyschoffen.mbt.alpha.shell :as shell]
    [com.jeremyschoffen.mbt.alpha.utils :as u]
    [com.jeremyschoffen.java.nio.file :as fs]))



;; inspired by https://github.com/EwenG/badigeon/blob/master/src/badigeon/sign.clj


(defn make-sign-cmd [{p-in :gpg.sign/in
                      p-out :gpg.sign/out
                      gpg-key :gpg/key-id}]
  (cond-> ["gpg"]
          p-out (conj "--output" (str p-out))
          :always (conj "-ab")
          gpg-key (conj "--default-key" (clojure.string/trim gpg-key))
          :always (conj (str p-in))))

(u/spec-op make-sign-cmd
           :param {:req [:gpg.sign/in]
                   :opt [:gpg.sign/out
                         :gpg/key-id]})


(defn make-sign-out [in]
  (let [p (fs/parent in)
        n (-> in
              fs/file-name
              (str ".asc"))]
    (if p
      (fs/path p n)
      (fs/path n))))


(defn ensure-sign-out [{in :gpg.sign/in
                        out :gpg.sign/out
                        :as param}]
  (cond-> param
          (not out) (assoc :gpg.sign/out (make-sign-out in))))

(u/simple-fdef ensure-sign-out
               :gpg.sign/spec)

(defn sign-file! [{wd  :project/working-dir
                   spec :gpg.sign/spec}]
  (let [spec (ensure-sign-out spec)]
    (assoc spec
      :shell/result
      (shell/safer-sh {:project/working-dir wd
                       :shell/command (make-sign-cmd spec)}))))

(u/spec-op sign-file!
           :deps [make-sign-cmd shell/safer-sh]
           :param {:req [:project/working-dir
                         :gpg.sign/spec]})


(defn sign-files! [{specs :gpg.sign/specs
                    :as param}]
  (mapv (fn [spec]
          (sign-file!
            (-> param
                (dissoc :gpg.sign/specs)
                (assoc :gpg.sign/spec spec))))
        specs))

(u/spec-op sign-files!
           :deps [sign-file!]
           :param {:req [:gpg.sign/specs]
                   :opt [:project/working-dir]})

(comment
  (sign-files! {:project/working-dir (u/safer-path)
                :gpg.sign/specs [{:gpg.sign/in (u/safer-path "target" "pom.xml")
                                  :gpg.sign/out (u/safer-path "target" "toto.asc")
                                  :gpg/key-id "D96BB413"}
                                 {:gpg.sign/in (u/safer-path "target" "pom.xml")}]})

  (ensure-sign-out {:gpg.sign/in (u/safer-path "target" "pom.xml")})
  (ensure-sign-out {:gpg.sign/in (fs/path "pom.xml")}))