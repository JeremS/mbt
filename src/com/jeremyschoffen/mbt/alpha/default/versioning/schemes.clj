(ns com.jeremyschoffen.mbt.alpha.default.versioning.schemes
  (:require
    [com.jeremyschoffen.mbt.alpha.core.specs]
    [com.jeremyschoffen.mbt.alpha.default.versioning.schemes.protocols :as vp]
    [com.jeremyschoffen.mbt.alpha.core.utils :as u]))




(defn initial-version [{h :versioning/scheme}]
  (vp/initial-version h))

(u/spec-op initial-version
           :param {:req [:versioning/scheme]})


(defn current-version [{s :versioning/scheme
                        desc :git/description}]
  (vp/current-version s desc))

(u/spec-op current-version
           :param {:req [:versioning/scheme :git/description]})


(defn bump [{s :versioning/scheme
             v :versioning/version
             l :versioning/bump-level}]
  (if l
    (vp/bump s v l)
    (vp/bump s v)))

(u/spec-op bump
           :param {:req [:versioning/scheme :versioning/version]
                   :opt [:versioning/bump-level]})
