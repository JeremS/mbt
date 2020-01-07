(ns com.jeremyschoffen.mbt.api.version.protocols
  "Handle abstract versions as a set of malleable components")

(defprotocol VersionScheme
  (initial-version [this])
  (current-version [this state])
  (bump [this version] [this version level]))
