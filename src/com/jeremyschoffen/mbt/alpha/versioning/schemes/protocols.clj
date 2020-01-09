(ns com.jeremyschoffen.mbt.alpha.versioning.schemes.protocols)

(defprotocol VersionScheme
  (initial-version [this])
  (current-version [this state])
  (bump [this version] [this version level]))
