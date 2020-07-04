(ns com.jeremyschoffen.mbt.alpha.default.building
  (:require
    [com.jeremyschoffen.java.nio.file :as fs]
    [com.jeremyschoffen.mbt.alpha.core :as mbt-core]
    [com.jeremyschoffen.mbt.alpha.default.building.jar :as jar]
    [com.jeremyschoffen.mbt.alpha.default.versioning :as v]
    [com.jeremyschoffen.mbt.alpha.default.specs]
    [com.jeremyschoffen.mbt.alpha.utils :as u]))


(u/alias-fn make-manifest-entry jar/make-manifest-entry)
(u/alias-fn make-deps-entry jar/make-deps-entry)
(u/alias-fn make-pom-entry jar/make-pom-entry)
(u/alias-fn make-staples-entries jar/make-staples-entries)
(u/alias-fn simple-jar-srcs jar/simple-jar-srcs)
(u/alias-fn uber-jar-srcs jar/uber-jar-srcs)


(defn ensure-jar-defaults [p]
  (u/ensure-computed p
    :project/deps mbt-core/get-deps
    :classpath/index mbt-core/indexed-classpath
    :maven/pom mbt-core/new-pom
    :jar/manifest mbt-core/make-manifest))

(u/spec-op ensure-jar-defaults
           :deps [mbt-core/indexed-classpath
                  mbt-core/make-manifest
                  mbt-core/new-pom
                  mbt-core/get-deps]
           :param {:req #{:maven/artefact-name
                          :maven/group-id
                          :project/working-dir
                          :project/version}
                   :opt #{:jar/main-ns
                          :jar.manifest/overrides
                          :project/author
                          :project.deps/aliases},})


(defn make-jar&clean! [{out :project/output-dir
                        jar-out :jar/output
                        :as param}]
  (u/ensure-dir! out)
  (fs/delete-if-exists! jar-out)
  (let [res (atom nil)]
    (let [temp-out (fs/create-temp-directory! "temp-out_" {:dir out})]
      (-> param
          (assoc :jar/temp-output temp-out
                 :cleaning/target temp-out)
          (u/side-effect! #(reset! res (mbt-core/add-srcs! %)))
          (u/side-effect! mbt-core/make-jar-archive!)
          (u/side-effect! mbt-core/clean!))
      @res)))

(u/spec-op make-jar&clean!
           :deps [mbt-core/add-srcs!
                  mbt-core/make-jar-archive!
                  mbt-core/clean!]
           :param {:req [:project/working-dir
                         :jar/output
                         :jar/srcs]
                   :opt [:jar/exclude?]})


(defn jar-out [{jar-name :build/jar-name
                out :project/output-dir}]
  (u/safer-path out jar-name))

(u/spec-op jar-out
           :param {:req [:build/jar-name :project/output-dir]})


(defn jar! [param]
  (-> param
      (assoc :jar/srcs (jar/simple-jar-srcs param)
             :jar/output (jar-out param))
      make-jar&clean!))

(u/spec-op jar!
           :deps [jar-out jar/simple-jar-srcs make-jar&clean!]
           :param {:req [:build/jar-name
                         :classpath/index
                         :jar/manifest
                         :maven/artefact-name
                         :maven/group-id
                         :maven/pom
                         :project/deps
                         :project/output-dir
                         :project/working-dir]
                   :opt [:jar/exclude?]})


(defn uberjar-out [{jar-name :build/uberjar-name
                    out :project/output-dir}]
  (u/safer-path out jar-name))

(u/spec-op uberjar-out
           :param {:req [:build/uberjar-name :project/output-dir]})


(defn uberjar! [param]
  (-> param
      (assoc :jar/srcs (jar/uber-jar-srcs param)
             :jar/output (uberjar-out param))
      make-jar&clean!))

(u/spec-op uberjar!
           :deps [uberjar-out jar/uber-jar-srcs make-jar&clean!]
           :param {:req [:build/uberjar-name
                         :classpath/index
                         :jar/manifest
                         :maven/artefact-name
                         :maven/group-id
                         :maven/pom
                         :project/deps
                         :project/output-dir
                         :project/working-dir]
                   :opt [:jar/exclude?]})
