(ns com.jeremyschoffen.mbt.api.version.protocols)

(defprotocol VersionScheme
  (initial-version [this])
  (current-version [this state])
  (bump [this version] [this version level]))
