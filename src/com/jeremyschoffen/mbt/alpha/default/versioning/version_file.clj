(ns com.jeremyschoffen.mbt.alpha.default.versioning.version-file
  (:require
    [clojure.string :as string]
    [com.jeremyschoffen.java.nio.alpha.file :as fs]
    [com.jeremyschoffen.mbt.alpha.core.specs]
    [com.jeremyschoffen.mbt.alpha.default.specs]
    [com.jeremyschoffen.mbt.alpha.utils :as u]))


(defn make-version-file [{v :project/version
                          ns :version-file/ns}]

  (string/join "\n" [(format "(ns %s)" ns)
                     ""
                     (format "(def version \"%s\")" v)
                     ""]))

(u/spec-op make-version-file
           :param {:req [:project/version
                         :version-file/ns]})



(defn write-version-file! [{dest :version-file/path
                            :as   param}]
  (spit dest (make-version-file param))
  dest)

(u/spec-op write-version-file!
           :param {:req [:project/version
                         :version-file/path
                         :version-file/ns]}
           :ret fs/path?)



(comment
  (println (make-version-file {:project/version "1.2"
                               :version-file/ns "toto.titi"})))


