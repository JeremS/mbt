(ns com.jeremyschoffen.mbt.alpha.default
  (:require
    [com.jeremyschoffen.mbt.alpha.default.building :as building]
    [com.jeremyschoffen.mbt.alpha.default.defaults :as defaults]
    [com.jeremyschoffen.mbt.alpha.default.versioning :as versioning]
    [com.jeremyschoffen.mbt.alpha.utils :as u]))


(u/alias-fn make-conf defaults/make-context)
(u/alias-fn bump-tag! versioning/bump-tag!)

(u/alias-fn ensure-jar-defaults building/ensure-jar-defaults)
(u/alias-fn jar! building/jar!)
(u/alias-fn uberjar! building/uberjar!)