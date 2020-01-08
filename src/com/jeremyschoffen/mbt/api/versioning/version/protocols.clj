(ns com.jeremyschoffen.mbt.api.versioning.version.protocols)

(defprotocol VersionScheme
  (initial-version [this])
  (current-version [this state])
  (bump [this version] [this version level]))
