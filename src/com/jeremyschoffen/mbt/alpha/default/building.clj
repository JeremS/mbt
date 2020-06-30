(ns com.jeremyschoffen.mbt.alpha.default.building
  (:require
    [com.jeremyschoffen.java.nio.file :as fs]
    [com.jeremyschoffen.mbt.alpha.core :as mbt-core]
    [com.jeremyschoffen.mbt.alpha.default.building.jar :as jar]
    [com.jeremyschoffen.mbt.alpha.default.specs]
    [com.jeremyschoffen.mbt.alpha.utils :as u]))


(u/alias-fn make-manifest-entry jar/make-manifest-entry)
(u/alias-fn make-deps-entry jar/make-deps-entry)
(u/alias-fn make-pom-entry jar/make-pom-entry)
(u/alias-fn make-staples-entries jar/make-staples-entries)
(u/alias-fn simple-jar-srcs jar/simple-jar-srcs)
(u/alias-fn uber-jar-srcs jar/uber-jar-srcs)


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


(defn jar! [{jar-name :build/jar-name
             out :project/output-dir
             :as param}]
  (-> param
      (assoc :jar/srcs (jar/simple-jar-srcs param)
             :jar/output (u/safer-path out jar-name))
      make-jar&clean!))

(u/spec-op jar!
           :deps [jar/simple-jar-srcs make-jar&clean!]
           :param {:req [:classpath/index
                         :build/jar-name
                         :jar/manifest
                         :maven/artefact-name
                         :maven/group-id
                         :maven/pom
                         :project/deps
                         :project/working-dir]
                   :opt [:jar/exclude?
                         :jar/main-ns
                         :jar.manifest/overrides
                         :project/author]})


(defn uberjar! [{jar-name :build/uberjar-name
                 out :project/output-dir
                 :as param}]
  (-> param
      (assoc :jar/srcs (jar/uber-jar-srcs param)
             :jar/output (u/safer-path out jar-name))
      make-jar&clean!))

(u/spec-op uberjar!
           :deps [jar/simple-jar-srcs make-jar&clean!]
           :param {:req [:classpath/index
                         :build/uberjar-name
                         :jar/manifest
                         :maven/artefact-name
                         :maven/group-id
                         :maven/pom
                         :project/deps
                         :project/working-dir]
                   :opt [:jar/exclude?
                         :jar/main-ns
                         :jar.manifest/overrides
                         :project/author]})
