(ns com.jeremyschoffen.mbt.alpha.building.classpath
  (:require
    [clojure.spec.alpha :as s]
    [clojure.string :as string]
    [clojure.tools.deps.alpha :as deps]
    [clojure.tools.deps.alpha.util.maven :as maven]
    [com.jeremyschoffen.java.nio.file :as fs]
    [com.jeremyschoffen.mbt.alpha.specs]
    [com.jeremyschoffen.mbt.alpha.utils :as u]))


;; adapted from https://github.com/EwenG/badigeon/blob/master/src/badigeon/classpath.clj#L6
(defn raw-classpath [{project-deps :project/deps
                      aliases      :project.deps/aliases}]
  (let [deps-map (update project-deps :mvn/repos #(merge maven/standard-repos %))
        args-map (deps/combine-aliases deps-map aliases)]

    (-> (deps/resolve-deps deps-map args-map)
        (deps/make-classpath (:paths deps-map) args-map))))

(u/spec-op raw-classpath
           (s/keys :req [:project/deps]
                   :opt [:project.deps/aliases])
           :classpath/raw)


(defn jar? [path]
  (string/ends-with? path ".jar"))


(defn project-path? [wd path]
  (fs/ancestor? wd path))


(defn classify [wd path]
  (cond
    (not (fs/exists? path)) :classpath/inexistant
    (jar? path) :classpath/jar
    (fs/directory? path) (if (project-path? wd path)
                           :classpath/dir
                           :classpath/ext-dep)
    :else :classpath/file))

(defn index-classpath [cp wd]
  (-> cp
      (string/split (re-pattern (System/getProperty "path.separator")))
      (->> (into [] (comp
                      (map (partial fs/resolve wd))
                      (map str)))
           sort
           (group-by (partial classify wd)))))


(defn indexed-classpath [{wd :project/working-dir
                          :as param}]
  (-> param
      raw-classpath
      (index-classpath wd)))

(u/spec-op indexed-classpath
           (s/keys :req [:project/working-dir
                         :project/deps]
                   :opt [:project.deps/aliases])
           :classpath/index)


(comment
  (require '[clojure.tools.deps.alpha.reader :as deps-reader])
  (def state {:project/working-dir (u/safer-path)
              :project/deps (deps-reader/slurp-deps "deps.edn")})

  (raw-classpath {:project/deps (deps-reader/slurp-deps "deps.edn")})
  (indexed-classpath state))
