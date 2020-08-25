(ns ^{:author "Jeremy Schoffen"
      :doc "
Protocol used to make version schemes.
      "}
  fr.jeremyschoffen.mbt.alpha.default.versioning.schemes.protocols)

(defprotocol VersionScheme
  (initial-version [this] [this level])
  (current-version [this git-description])
  (bump [this version] [this version level]))
