(ns fr.jeremyschoffen.mbt.alpha.utils-test
  (:require
    [clojure.test :refer [deftest]]
    [clojure.spec.alpha :as s]
    [clojure.spec.test.alpha :as st]
    [testit.core :refer :all]
    [fr.jeremyschoffen.mbt.alpha.utils :as u]))



(s/def ::a any?)
(s/def ::b any?)
(s/def ::c any?)
(s/def ::d any?)

(defn foo [a])

(u/spec-op foo
           :param {:req [::a]
                   :opt [::c]})

(defn bar [a])

(u/spec-op bar
           :deps [foo]
           :param {:req [::b]
                   :opt [::d]})

(defn baz [x])

(u/spec-op baz
           :deps [bar])



(deftest specing-works
  (facts
    (:param (u/spec foo)) => {:req #{::a}
                              :opt #{::c}}
    (:param (u/spec bar)) => {:req #{::b}
                              :opt #{::d}}
    (:param (u/spec baz)) => {}

    (u/param-suggestions baz) =in=> (:param (u/spec bar))))


(st/instrument)
(deftest instrumenting-works
  (let [e (try (foo {})
               (catch Exception e e))]
    (fact (ex-data e)
          =in=> {:clojure.spec.alpha/failure :instrument
                 :clojure.spec.alpha/fn `foo})))
