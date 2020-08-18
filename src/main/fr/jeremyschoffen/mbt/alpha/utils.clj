(ns ^{:author "Jeremy Schoffen"
      :doc "
Utilities used in the whole project.
      "}
  fr.jeremyschoffen.mbt.alpha.utils
  (:require
    [clojure.spec.alpha :as s]
    [medley.core :as medley]
    [fr.jeremyschoffen.dolly.core :as dolly]
    [fr.jeremyschoffen.java.nio.alpha.file :as fs]))


;;----------------------------------------------------------------------------------------------------------------------
;; Some fs utils
;;----------------------------------------------------------------------------------------------------------------------
(defn safer-path
  ([]
   (safer-path "."))
  ([& args]
   (->> args
        (apply fs/path)
        fs/canonical-path)))


(defn ensure-dir! [d]
  (when (fs/not-exists? d)
    (fs/create-directories! d))
  d)


(defn ensure-parent! [f]
  (some-> f fs/parent ensure-dir!)
  f)

;;----------------------------------------------------------------------------------------------------------------------
;; Additional map utils
;;----------------------------------------------------------------------------------------------------------------------

(defn- check-kfs [kfs]
  (when-not (even? (count kfs))
    (throw (IllegalArgumentException.
             "Expected even number of arguments after map/vector, found odd number."))))


(defn assoc-computed [m & kfs]
  (check-kfs kfs)
  (reduce (fn [m [k f]]
            (assoc m k (f m)))
          m
          (partition 2 kfs)))


(defn ensure-computed [m & kfs]
  (check-kfs kfs)
  (reduce (fn [m [k f]]
            (if (contains? m k)
              m
              (assoc m k (f m))))
          m
          (partition 2 kfs)))


(defn- augment-computed*
  [m k f]
  (let [defaults (get m k)
        res (f m)]
    (assoc m k (medley/deep-merge defaults res))))


(defn augment-computed
  [m & kfs]
  (check-kfs kfs)
  (reduce (fn [m [k f]]
            (augment-computed* m k f))
          m
          (partition 2 kfs)))


(defn side-effect!
  "Make *`identity` with side effect* functions when constructing threaded code.

  The 1 argument version is a combinator taking a function `f!` and returning a function. The returned function
  is of 1 argument `v`, effects `(f! v) and returns `v` as it received it. The result is basically an `identity`
  function with a side effect inside it. This arity is intended to be used with
  [[fr.jeremyschoffen.mbt.alpha.utils/thread-fns]].

  The 2 argument version is intended to be used in the macro `->`. It applies `f!` to `v` then returns `v` unchanged.
  You can for instance:
  ```clojure
  (-> 1
      inc
      (side-effect! println)
      dec)
  ; 2
  ;=> 1
  ```"
  ([f!]
   (fn [v]
     (side-effect! v f!)))
  ([v f!]
   (f! v)
   v))


(defn check [v f!]
  (f! v)
  v)


(defn thread-fns
  "Function serving a similar purpose than the `->` macro. It will  thread a value `v` through a sequence of
  functions `fns` the result of one function application becoming the argument of the next.

  Args:
  - `v`: the value to be threaded
  - `fns`: 1 argument functions that will be applied."
  [v & fns]
  (reduce (fn [acc f]
            (f acc))
          v
          fns))


(defn strip-keys-nss [m]
  (medley/map-keys #(-> % name keyword) m))


;;----------------------------------------------------------------------------------------------------------------------
;; Utils
;;----------------------------------------------------------------------------------------------------------------------
(defmacro ensure-keys [m & kvs]
  (assert (even? (count kvs)))
  (let [res (gensym "res")]
    `(let [~res ~m
           ~@(apply concat (for [[k v] (partition 2 kvs)]
                             `[~res (if (contains? ~res ~k)
                                      ~res
                                      (assoc ~res ~k ~v))]))]
       ~res)))


(defmacro ensure-key [m k v]
  (list `ensure-keys m k v))


;;----------------------------------------------------------------------------------------------------------------------
;; Specs utils
;;----------------------------------------------------------------------------------------------------------------------
(defmacro simple-fdef
  "Macro to spec 1-arg functions. "
  ([n param-spec]
   (list `simple-fdef n param-spec nil))
  ([n param-spec ret]
   (let [spec (cond-> [:args (list `s/cat :param param-spec)]
                      ret (conj :ret ret))]
     `(s/fdef ~n ~@spec))))

;; taken from clojure.spec.alpha/ns-qualify
(defn- ns-qualify
  "Qualify symbol s by resolving it or using the current *ns*."
  [s]
  (if-let [ns-sym (some-> s namespace symbol)]
    (or (some-> (get (ns-aliases *ns*) ns-sym)
                str
                (symbol (name s)))
        s)
    (symbol (str (.name *ns*)) (str s))))


(def param-specs-store (atom {}))


(defn add-spec! [fn-name spec]
  (swap! param-specs-store assoc fn-name spec))


(defn- make-spec-form [{:keys [req opt req-un opt-un]}]
  (list `s/keys :req (vec req)
        :opt (vec opt)
        :req-un (vec req-un)
        :opt-un (vec opt-un)))


(defn- make-defn-spec-form [n param-spec ret-spec]
  `(s/fdef ~n
           :args (s/cat :param ~(make-spec-form param-spec))
           :ret ~ret-spec))


(defmacro spec-op [n & {:keys [deps param ret]
                        :or  {deps #{}
                              param {}
                              ret 'any?}
                        :as  spec}]
  (let [sanitized (-> spec
                      (update :deps #(set (mapv ns-qualify %)))
                      (update-in [:param :req] set)
                      (update-in [:param :opt] set)
                      (update-in [:param :req-un] set)
                      (update-in [:param :opt-un] set))]
    `(do
       (add-spec! '~(ns-qualify n) '~sanitized)
       ~(make-defn-spec-form n param ret))))


(s/def ::param  map?)
(s/def ::deps (s/coll-of symbol?))
(s/def ::ret any?)


(s/def ::spec-op-opts (s/and (s/map-of #{:param :deps :ret} any?)
                             (s/keys :opt-un [::param ::deps ::ret])))
(s/def ::spec-op-args (s/and (s/cat :name symbol?
                                    :opts (s/* any?))
                             (fn [c]
                               (s/valid? ::spec-op-opts (apply hash-map (:opts c))))))

(s/fdef spec-op
        :args ::spec-op-args)


(defn get-spec [sym]
  (get @param-specs-store sym))


(defn get-deps* [specs-map sym]
  (get-in specs-map [sym :deps] #{}))


(defn- merge-param-specs* [param-specs type]
  (into (sorted-set)
        (mapcat type)
        param-specs))


(defn- merge-param-specs [param-specs]
  {:req (merge-param-specs* param-specs :req)
   :opt (merge-param-specs* param-specs :opt)
   :req-un (merge-param-specs* param-specs :req-un)
   :opt-un (merge-param-specs* param-specs :opt-un)})


(defn get-param-specs-suggestions* [spec-map sym]
  (let [deps (get-deps* spec-map sym)]
    (with-meta (->> deps
                    (into [] (comp (map (partial get spec-map))
                                   (map :param)))
                    merge-param-specs)
               {:details (select-keys spec-map deps)})))


(defn get-param-specs-suggestions [sym]
  (get-param-specs-suggestions* @param-specs-store sym))


(defmacro param-suggestions
  "Search the function spec and the declared dependencies specs transitively to
  find all the potential params the function may require."
  [sym]
  `(get-param-specs-suggestions '~(ns-qualify sym)))


(defn get-param-specs [sym]
  {:spec (get-spec sym)
   :transitive-suggestions (get-param-specs-suggestions sym)})


(defmacro param-specs
  "Search the function spec and the declared dependencies specs transitively to
  find all the potential params the function may require."
  [sym]
  `(get-param-specs '~(ns-qualify sym)))


(defn requirer [kw]
  (into #{}
        (keep (fn [[f spec]]
                (let [param-spec (:param spec)
                      {:keys [req opt]} param-spec]
                  (when (or (contains? req kw)
                            (contains? opt kw))
                    f))))
        @param-specs-store))


;;----------------------------------------------------------------------------------------------------------------------
;; Dolly stuff.
;;----------------------------------------------------------------------------------------------------------------------
(defmacro def-clone [new-name cloned]
  (let [{:keys [type cloned-sym]} (dolly/cloned-info cloned)]
    `(do
       (dolly/def-clone ~new-name ~cloned-sym)
       ~@(when (= type :function)
           `[(s/def ~new-name (clojure.spec.alpha/get-spec '~cloned-sym))
             (add-spec! '~(ns-qualify new-name) (get-spec '~cloned-sym))])
       (var ~new-name))))
