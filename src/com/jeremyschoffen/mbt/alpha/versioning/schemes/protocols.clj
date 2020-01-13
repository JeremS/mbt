(ns com.jeremyschoffen.mbt.alpha.versioning.schemes.protocols)

(defprotocol VersionScheme
  (initial-version [this])
  (current-version [this git-description])
  (bump [this version] [this version level]))
