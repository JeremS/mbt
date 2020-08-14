(ns ^{:author "Jeremy Schoffen"
      :doc "
Api used to generate version files.
      "}
  fr.jeremyschoffen.mbt.alpha.default.versioning.version-file
  (:require
    [clojure.string :as string]
    [fr.jeremyschoffen.java.nio.alpha.file :as fs]
    [fr.jeremyschoffen.mbt.alpha.core.specs]
    [fr.jeremyschoffen.mbt.alpha.default.specs]
    [fr.jeremyschoffen.mbt.alpha.utils :as u]))


(defn version-file-content
  "Make the string content of a version file."
  [{v :project/version
    ns   :version-file/ns}]
  (string/join "\n" [(format "(ns %s)" ns)
                     ""
                     (format "(def version \"%s\")" v)
                     ""]))

(u/spec-op version-file-content
           :param {:req [:project/version
                         :version-file/ns]})


(defn write-version-file!
  "Make the string content of a version file and spit it at the destination specified under the key
  `:version-file/path`."
  [{dest :version-file/path
    :as   param}]
  (spit dest (version-file-content param))
  dest)

(u/spec-op write-version-file!
           :param {:req [:project/version
                         :version-file/path
                         :version-file/ns]}
           :ret fs/path?)
