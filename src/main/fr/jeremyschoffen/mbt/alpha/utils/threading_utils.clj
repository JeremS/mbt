(ns fr.jeremyschoffen.mbt.alpha.utils.threading-utils
  (:require
    [clojure.tools.logging :as log]
    [fr.jeremyschoffen.mapiform.alpha.clj.spec :as mapi-spec]
    [fr.jeremyschoffen.mapiform.alpha.core :as mapi-core])
  (:import (clojure.lang ExceptionInfo)))


;;----------------------------------------------------------------------------------------------------------------------
;; Middleware
;;----------------------------------------------------------------------------------------------------------------------
;; Stack
(defn push-name-stack [ctxt name]
  (vary-meta ctxt update ::stack (fnil conj []) name))


(defn pop-name-stack [ctxt]
  (vary-meta ctxt update ::stack pop))


(defn current-stack [ctxt]
  (-> ctxt meta ::stack))


(defn current-name [ctxt]
  (-> ctxt current-stack peek))


(defn wrap-name
  "Middleware pushing and popping a name onto a stack situated in the metadata of
  the `ctxt`. When threading it allows the following of where in the called stack we are.
  The names "
  [f! name]
  (fn [ctxt]
    (-> ctxt
        (push-name-stack name)
        f!)))

;;----------------------------------------------------------------------------------------------------------------------
;; Dry run
(defn make-dry-run
  "Specify to function using `ctxt` to work in dry run mode by altering the context's metadata"
  [ctxt]
  (vary-meta ctxt assoc ::dry-run true))


(defn dry-run? [ctxt]
  (-> ctxt meta ::dry-run))


(defn wrap-dry-run [f!]
  (fn [ctxt]
    (if (dry-run? ctxt)
      ::dry-run
      (f! ctxt))))

;;----------------------------------------------------------------------------------------------------------------------
;; Recording
(def ^:dynamic *recording* nil)


(defn wrap-record
  "Middleware used to record side effects and theirs results. It uses the dynamic var
  [[fr.jeremyschoffen.mbt.alpha.default.threading-utils/*recording*]].

  To record, this var must be bound to `(atom [])` surrounding the
  code being recorded."
  [f!]
  (fn [ctxt]
    (let [res (f! ctxt)]
      (when *recording*
        (swap! *recording* conj {:stack (current-stack ctxt)
                                 :called (current-name ctxt)
                                 :ctxt ctxt
                                 :ret res}))
      res)))


(defmacro record
  "Capture recording of side effects wrapped with
  [[fr.jeremyschoffen.mbt.alpha.default.threading-utils/wrap-record]]."
  [& body]
  `(binding [*recording* (atom [])]
     (let [res# (try
                  (do ~@body)
                  (catch ExceptionInfo e#
                    (throw (ex-info (ex-message e#)
                                    (assoc (ex-data e#)
                                      :recording @*recording*)
                                    (ex-cause e#))))
                  (catch Exception e#
                    (throw (ex-info (.getMessage e#)
                                    {:recording @*recording*}
                                    e#))))]
       {:res res#
        :recording @*recording*})))

(defn rank [recorded-se]
  (-> recorded-se :stack count))


(defn format-recording
  "Transform a record of side effects into a tree that models the succession od events better."
  {:arglists '([recorded-side-effects])}
  ([recorded-ses]
   (format-recording recorded-ses []))
  ([recorded-ses stack]
   (if (empty? recorded-ses)
     stack
     (let [[f] recorded-ses
           current-rank (rank f)
           [equals [pivot & rst :as diffs]] (split-with
                                              #(= current-rank (rank %))
                                              recorded-ses)]
       (cond
         (not pivot)
         (into stack equals)

         (> current-rank (rank pivot))
         (recur (cons (assoc pivot :children equals) rst) stack)

         :else
         (recur diffs (into stack equals)))))))

;;----------------------------------------------------------------------------------------------------------------------
;; Logging
(defn relevant-keys [name]
  (-> name
      mapi-spec/get-report
      (get-in [:spec :param])
      vals
      (->> (apply clojure.set/union))))


(defn wrap-generic-logging
  "Middleware used to provide some logging."
  [f!]
  (fn [ctxt]
    (let [n (current-name ctxt)
          focused-ctxt (select-keys ctxt (relevant-keys n))]
      (log/info "Calling: " n)
      (log/debug "stack:" (current-stack ctxt))
      (log/debug "conf: "
                 (clojure.string/trim
                   (with-out-str
                     (clojure.pprint/pprint focused-ctxt))))
      (let [res (f! ctxt)]
        (log/debug "res:" res)
        res))))

;;----------------------------------------------------------------------------------------------------------------------
;; Error
(defn wrap-error
  "Middleware used to wrap side effects in a way that it augments their
  exceptions with some data."
  [f!]
  (fn [ctxt]
    (let [n (current-name ctxt)]
      (try
        (f! ctxt)
        (catch ExceptionInfo e
          (throw (ex-info (ex-message e)
                          (-> e
                              ex-data
                              (update ::call-stack (fnil conj []) n))
                          (ex-cause e))))
        (catch Exception e
          (throw (ex-info (.getMessage e)
                          {::call-stack [n]}
                          e)))))))

;;----------------------------------------------------------------------------------------------------------------------
;; Utils
;;----------------------------------------------------------------------------------------------------------------------
;; branching out
(defn wrap-branch*
  "Wraps a function with the following middleware:
  - wrap-generic-logging
  - wrap-record
  - wrap-name"
  [f n]
  (-> f
      wrap-generic-logging
      wrap-record
      (wrap-name n)))

(defn branch-named!
  "In a threaded operation, will execute `f!` to the side
  and return `v` as it got it.

  Wraps `f! with` [[fr.jeremyschoffen.mbt.alpha.default.threading-utils/wrap-branch*]].

  For the middleware used, the operation is named `n`."
  ([f! n]
   (mapi-core/side-effect! (wrap-branch* f! n)))
  ([v f! n]
   (mapi-core/side-effect! v (wrap-branch* f! n))))


(defn- make-name [s]
  (assert (symbol? s) (str "Wrapping of a non symbol " s))
  (-> s resolve symbol))


(defmacro branch!
  "Similar to [[fr.jeremyschoffen.mbt.alpha.default.threading-utils/branch-named!]].
  `f!` is expected to be a symbol that resolves to a function. The name of the branch
  will then be `(resolve f!)`."
  ([f!]
   `(branch-named! ~f! '~(make-name f!)))
  ([v f!]
   `(branch-named! ~v ~f! '~(make-name f!))))


;;----------------------------------------------------------------------------------------------------------------------
;; branching to a side effect
(defn wrap-side-effect*
  "Wraps a function understood to be a side-effect from the core api with the following middleware:
  - wrap-dry-run
  - wrap-generic-logging
  - wrap-error
  - wrap-record
  - wrap-name"
  [f n]
  (-> f
      wrap-dry-run
      wrap-generic-logging
      wrap-error
      wrap-record
      (wrap-name n)))


(defn do-side-effect-named!
  "Same as [[fr.jeremyschoffen.mbt.alpha.default.threading-utils/branch-named!]] except for the middleware used to wrap
  `f!`.

  Here [[fr.jeremyschoffen.mbt.alpha.default.threading-utils/wrap-side-effect*]] is used."
  ([f! n]
   (mapi-core/side-effect! (wrap-side-effect* f! n)))
  ([v f! n]
   (mapi-core/side-effect! v (wrap-side-effect* f! n))))


(defmacro do-side-effect!
  " Simmilar to [[fr.jeremyschoffen.mbt.alpha.default.threading-utils/branch!]] but using the
  [[fr.jeremyschoffen.mbt.alpha.default.threading-utils/wrap-side-effect*]] middelware."
  ([f!]
   `(do-side-effect-named! ~f! '~(make-name f!)))
  ([v f!]
   `(do-side-effect-named! ~v ~f! '~(make-name f!))))
