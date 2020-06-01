(ns com.jeremyschoffen.mbt.alpha.core.utils
  (:require
    [clojure.set :as set]
    [clojure.spec.alpha :as s]
    [cognitect.anomalies :as anom]
    [com.jeremyschoffen.java.nio.file :as fs]))


(def maven-default-settings-file (fs/path (System/getProperty "user.home") ".m2" "settings.xml"))


(System/getProperty "maven.conf")
(System/getProperty "org.apache.maven.global-settings")


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
;; totally riped from clojure core...
(defn assoc-computed
  ([m k f]
   (assoc m k (f m)))
  ([m k f & kfs]
   (let [ret (assoc-computed m k f)]
     (if kfs
       (if (next kfs)
         (recur ret (first kfs) (second kfs) (nnext kfs))
         (throw (IllegalArgumentException.
                  "assoc-computed expects even number of arguments after map/vector, found odd number")))
       ret))))


(defn merge-computed [m f]
  (merge m (f m)))


(defn side-effect! [v f!]
  (f! v)
  v)


(defn check [v f!]
  (f! v)
  v)

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
    (or (some-> (get (ns-aliases *ns*) ns-sym) str (symbol (name s)))
        s)
    (symbol (str (.name *ns*)) (str s))))


(def param-specs-store (atom {}))


(defn add-spec! [fn-name spec]
  (swap! param-specs-store assoc fn-name spec))


(defn- make-spec-form [{:keys [req opt]}]
  (list `s/keys :req (vec req)
                :opt (vec opt)))


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
                      (update-in [:param :opt] set))]
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


(defn get-all-deps* [specs-map sym]
  (loop [res #{sym}]
    (let [t (into #{} (mapcat (partial get-deps* specs-map)) res)
          new-deps (set/difference t res)
          new-res (set/union res new-deps)]
      (if (not-empty new-deps)
        (recur new-res)
        (disj res sym)))))


(defn- merge-param-specs* [param-specs type]
  (into (sorted-set)
        (mapcat type)
        param-specs))


(defn- merge-param-specs [param-specs]
  {:req (merge-param-specs* param-specs :req)
   :opt (merge-param-specs* param-specs :opt)})


(defn get-param-specs-suggestions* [spec-map sym]
  (->> sym
       (get-all-deps* spec-map)
       (into [] (comp (map (partial get spec-map))
                      (map :param)))
       merge-param-specs))


(defn get-param-specs-suggestions [sym]
  (get-param-specs-suggestions* @param-specs-store sym))


(defmacro param-suggestions
  "Search the function spec and the declared dependencies specs transitively to
  find all the potential params the function may require."
  [sym]
  `(get-param-specs-suggestions '~(ns-qualify sym)))


(defn get-param-specs [sym]
  {:spec (get-spec sym)
   :transitive-suggections (get-param-specs-suggestions sym)})


(defmacro param-specs
  "Search the function spec and the declared dependencies specs transitively to
  find all the potential params the function may require."
  [sym]
  `(get-param-specs '~(ns-qualify sym)))


