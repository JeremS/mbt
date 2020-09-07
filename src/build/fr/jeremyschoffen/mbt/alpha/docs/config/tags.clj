(ns fr.jeremyschoffen.mbt.alpha.docs.config.tags
  (:require
    [clojure.spec.alpha :as s]
    [clojure.data.xml :as xml]
    [fr.jeremyschoffen.textp.alpha.lib.tag-utils :as textp-lib]
    [fr.jeremyschoffen.mbt.alpha.docs.config.data :as config-data]
    [fr.jeremyschoffen.mbt.alpha.utils :as u]))

(u/pseudo-nss
  docs.tags)

(s/def ::config-key-args (s/cat :config-key (s/? ::textp-lib/tag-clj-arg)
                                :doc (s/? ::textp-lib/tag-txt-arg)))



(textp-lib/conform-or-throw ::config-key-args [{:tag :tag-args-clj
                                                :content [:a-spec]}
                                               {:tag :tag-args-txt
                                                :content ["a description"]}])

(defn config-key-args->full-description [args]
  (let [conformed (textp-lib/conform-or-throw ::config-key-args args)
        config-key (-> conformed :config-key :content first)

        doc (-> conformed :doc :content)]
    (-> config-key
        config-data/get-description
        (assoc :config-key/documentation doc))))


(defn blank-space? [x]
  (and (string? x)
       (empty? (clojure.string/trim x))))

(defn config-key [& args]
  (let [{:config-key/keys [name constructors spec documentation]}
        (config-key-args->full-description args)]
    (xml/sexp-as-element [::docs.tags/config-key
                          [:h2 (format " `%s`" name)]

                          [:h3 "Spec:"]
                          [:md-block {:type "clojure"}
                           (-> spec
                               clojure.pprint/pprint
                               with-out-str
                               clojure.string/trim)]

                          (when (and (seq documentation)
                                     (every? #(not (blank-space? %))
                                             documentation))
                            (list
                              [:h3 "Description:"]
                              (vec (cons :p documentation))))


                          (when (seq constructors)
                            (list
                              [:h3 "Constructors:"]
                              [:ul (for [c constructors]
                                     [:li (format "[[%s]]" c)])]))])))
