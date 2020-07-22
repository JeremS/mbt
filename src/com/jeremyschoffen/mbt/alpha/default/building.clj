(ns com.jeremyschoffen.mbt.alpha.default.building
  (:require
    [com.jeremyschoffen.java.nio.alpha.file :as fs]
    [com.jeremyschoffen.mbt.alpha.core :as mbt-core]
    [com.jeremyschoffen.mbt.alpha.default.building.jar :as jar]
    [com.jeremyschoffen.mbt.alpha.default.versioning :as v]
    [com.jeremyschoffen.mbt.alpha.default.specs]
    [com.jeremyschoffen.mbt.alpha.utils :as u]))


(defn ensure-jar-defaults
  "Adds to a config map the necessary keys to make a jar. Namely:
    - :project/deps
    - :classpath/index
    - :maven/pom
    - :jar/manifest"
  [p]
  (u/ensure-computed p
                     :project/deps mbt-core/deps-get
                     :classpath/index mbt-core/classpath-indexed
                     :maven/pom mbt-core/maven-new-pom
                     :jar/manifest mbt-core/manifest))

(u/spec-op ensure-jar-defaults
           :deps [mbt-core/classpath-indexed
                  mbt-core/manifest
                  mbt-core/maven-new-pom
                  mbt-core/deps-get]
           :param {:req #{:maven/artefact-name
                          :maven/group-id
                          :project/working-dir
                          :project/version}
                   :opt #{:jar/main-ns
                          :jar.manifest/overrides
                          :project/author
                          :project.deps/aliases},})


(defn make-jar&clean!
  "Create a jar, simplying the process by handling the creation and deletion of the tempout put that will be zipped
  into the result jar."
  [{out :project/output-dir
    jar-out :jar/output
    :as param}]
  (u/ensure-dir! out)
  (fs/delete-if-exists! jar-out)
  (let [res (atom nil)]
    (let [temp-out (fs/create-temp-directory! "temp-out_" {:dir out})]
      (-> param
          (assoc :jar/temp-output temp-out
                 :cleaning/target temp-out)
          (u/side-effect! #(reset! res (mbt-core/jar-add-srcs! %)))
          (u/side-effect! mbt-core/jar-make-archive!)
          (u/side-effect! mbt-core/clean!))
      @res)))

(u/spec-op make-jar&clean!
           :deps [mbt-core/jar-add-srcs!
                  mbt-core/jar-make-archive!
                  mbt-core/clean!]
           :param {:req [:project/working-dir
                         :jar/output
                         :jar/srcs]
                   :opt [:jar/exclude?]})


(defn jar-out
  "Make the jar path given the `:project/output-dir` and `:build/jar-name`."
  [{jar-name :build/jar-name
    out :project/output-dir}]
  (u/safer-path out jar-name))

(u/spec-op jar-out
           :param {:req [:build/jar-name :project/output-dir]}
           :ret :jar/output)


(defn jar!
  "Create a skinny jar. The jar sources are determined using
  `com.jeremyschoffen.mbt.alpha.default.building.jar/simple-jar-srcs`, the jar's path name `jar-out`."
  [param]
  (-> param
      (u/ensure-computed
        :jar/srcs jar/simple-jar-srcs
        :jar/output jar-out)
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


(defn uberjar-out
  "Make the uberjar path given the `:project/output-dir` and `:build/jar-name`."
  [{jar-name :build/uberjar-name
    out :project/output-dir}]
  (u/safer-path out jar-name))

(u/spec-op uberjar-out
           :param {:req [:build/uberjar-name :project/output-dir]}
           :ret :jar/output)


(defn uberjar!
  "Build an uberjar. The jar sources are determined using
  `com.jeremyschoffen.mbt.alpha.default.building.jar/uber-jar-srcs`, the uberjar's path `uberjar-out`."
  [param]
  (-> param
      (u/ensure-computed
        :jar/srcs jar/uber-jar-srcs
        :jar/output uberjar-out)
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
