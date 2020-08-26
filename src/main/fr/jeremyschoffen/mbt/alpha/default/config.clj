(ns fr.jeremyschoffen.mbt.alpha.default.config
 (:require
   [juxt.clip.core :as clip]
   [fr.jeremyschoffen.mbt.alpha.utils :as u]))

(u/mbt-alpha-pseudo-nss
  project)

(defn target-dir [{wd ::project/working-dir}]
  (u/safer-path wd "target"))


(def conf
  {::project/working-dir `(u/safer-path)
   ::project/output-dir `(target-dir {::project/working-dir (clip/ref ::project/working-dir)})})

(defn conf->clip-conf [conf]
  {:components
   (into {}
         (map (fn [[k v]]
                  [k {:start v}]))
         conf)})

(clip/start (conf->clip-conf conf))



(def clip-conf
  {:components
   {::project/working-dir {:start `(u/safer-path)}
    ::project/output-dir {:start `(u/safer-path (clip/ref ::project/working-dir))}}})


(clip/start conf)