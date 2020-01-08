(ns com.jeremyschoffen.mbt.api.versioning.schemes.protocols)

(defprotocol VersionScheme
  (initial-version [this])
  (current-version [this state])
  (bump [this version] [this version level]))
