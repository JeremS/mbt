(ns fr.jeremyschoffen.mbt.alpha.utils.threading-utils-test
  (:require
    [clojure.test :refer :all]
    [testit.core :refer :all]
    [fr.jeremyschoffen.mapiform.alpha.core :as mapi-core]
    [fr.jeremyschoffen.mbt.alpha.utils.threading-utils :as tu]))


(def db (atom []))

(defn reset-db! []
  (reset! db []))

(defn out! [x]
  (swap! db conj x))


(defn thread-it [ctxt]
  (mapi-core/thread-fns ctxt
                        #(assoc % :c 10)
                        (tu/do-side-effect! out!)
                        (tu/do-side-effect-named! #(assoc % :d 25) :tutu)))

(defn example-dry []
  (-> {:a 1}
      tu/make-dry-run
      (tu/do-side-effect! out!)
      (assoc :b 2)
      (tu/do-side-effect-named! out! :titi)
      (tu/branch! thread-it)))


(deftest dry-run
  (let [_ (reset-db!)
        res (tu/record
              (example-dry))]
    (is (= res
           {:recording [{:called `out!
                         :ctxt {:a 1}
                         :ret ::tu/dry-run
                         :stack [`out!]}

                        {:called :titi
                         :ctxt {:a 1 :b 2}
                         :ret ::tu/dry-run
                         :stack [:titi]}

                        {:called `out!
                         :ctxt {:a 1 :b 2 :c 10}
                         :ret ::tu/dry-run
                         :stack [`thread-it `out!]}

                        {:called :tutu
                         :ctxt {:a 1 :b 2 :c 10}
                         :ret ::tu/dry-run
                         :stack [`thread-it :tutu]}

                        {:called `thread-it
                         :ctxt {:a 1 :b 2}
                         :ret {:a 1 :b 2 :c 10}
                         :stack [`thread-it]}]
            :res {:a 1 :b 2}}))
    (is (= @db []))))


(defn example []
  (-> {:a 1}
      (tu/do-side-effect! out!)
      (assoc :b 2)
      (tu/do-side-effect-named! out! :titi)
      (tu/branch! thread-it)))


(deftest real-run
  (let [_ (reset-db!)
        res (tu/record
              (example))]

    (is (= res
           {:recording [{:called `out!
                         :ctxt {:a 1}
                         :ret [{:a 1}]
                         :stack [`out!]}

                        {:called :titi
                         :ctxt {:a 1 :b 2}
                         :ret [{:a 1} {:a 1 :b 2}]
                         :stack [:titi]}

                        {:called `out!
                         :ctxt {:a 1 :b 2 :c 10}
                         :ret [{:a 1} {:a 1 :b 2} {:a 1 :b 2 :c 10}]
                         :stack [`thread-it `out!]}

                        {:called :tutu
                         :ctxt {:a 1 :b 2 :c 10}
                         :ret {:a 1 :b 2 :c 10 :d 25}
                         :stack [`thread-it :tutu]}

                        {:called `thread-it
                         :ctxt {:a 1 :b 2}
                         :ret {:a 1 :b 2 :c 10}
                         :stack [`thread-it]}]
            :res {:a 1 :b 2}}))

    (is (= @db
           [{:a 1} {:a 1 :b 2} {:a 1 :b 2 :c 10}]))

    (is (= (tu/format-recording (:recording res))
           [{:called `out!
             :ctxt {:a 1}
             :ret [{:a 1}]
             :stack [`out!]}

            {:called :titi
             :ctxt {:a 1 :b 2}
             :ret [{:a 1} {:a 1 :b 2}]
             :stack [:titi]}

            {:called `thread-it
             :ctxt {:a 1 :b 2}
             :ret {:a 1 :b 2 :c 10}
             :stack [`thread-it]
             :children [{:called `out!
                         :ctxt {:a 1 :b 2 :c 10}
                         :ret [{:a 1} {:a 1 :b 2} {:a 1 :b 2 :c 10}]
                         :stack [`thread-it `out!]}

                        {:called :tutu
                         :ctxt {:a 1 :b 2 :c 10}
                         :ret {:a 1 :b 2 :c 10 :d 25}
                         :stack [`thread-it :tutu]}]}]))))


