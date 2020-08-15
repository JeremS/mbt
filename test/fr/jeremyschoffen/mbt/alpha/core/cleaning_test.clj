(ns fr.jeremyschoffen.mbt.alpha.core.cleaning-test
  (:require
    [clojure.test :refer [deftest testing]]
    [clojure.spec.test.alpha :as stest]
    [testit.core :refer :all]
    [cognitect.anomalies :as anom]
    [fr.jeremyschoffen.java.nio.alpha.file :as fs]
    [fr.jeremyschoffen.mbt.alpha.core.cleaning :as cleaning]))



(stest/instrument [cleaning/clean!])


(deftest cleaning
  (let [wd (fs/create-temp-directory! "wd")
        target-dir (fs/path wd "target")
        dir1 (fs/path target-dir "dir1")
        dir2 (fs/path target-dir "dir2")
        file-1-1 (fs/path dir1 "file-1-1")
        file-2-1 (fs/path dir2 "file-2-1")

        all-files #{target-dir dir1 dir2 file-1-1 file-2-1}
        make-all #(do
                    (fs/create-directories! dir1)
                    (fs/create-directories! dir2)
                    (spit file-1-1 "1-1")
                    (spit file-2-1 "2-1"))]

    (testing "Throws when the target doesn't exist."
      (fact
        (cleaning/clean! {:project/working-dir wd
                          :cleaning/target target-dir})
        =throws=> (ex-info? "File to clean doesn't exist."
                            {::anom/category ::anom/not-found
                             :cleaning/target target-dir})))

    (testing "All the files are created."
      (make-all)
      (fact
        (every? fs/exists? all-files) => true))

    (testing "We catch errors."
      (facts
        (cleaning/clean! {:project/working-dir wd
                          :cleaning/target wd})
        =throws=> (ex-info? identity {::anom/category ::anom/forbidden
                                      :mbt/error :deleting-project-directory})

        (cleaning/clean! {:project/working-dir target-dir
                          :cleaning/target wd})
        =throws=> (ex-info? identity {::anom/category ::anom/forbidden
                                      :mbt/error :deletion-outside-working-dir})))

    (testing "Nothing remains after deletion."
      (cleaning/clean! {:project/working-dir wd
                        :cleaning/target target-dir})

      (fact
        (every? (complement fs/exists?) all-files) => true))))
