(ns docs.config.generate
  (:require
    [meander.epsilon :as m]
    [fr.jeremyschoffen.textp.alpha.reader.core :as textp-reader]
    [fr.jeremyschoffen.textp.alpha.lib.compilation :refer [emit!] :as compi]
    [fr.jeremyschoffen.mbt.alpha.utils :as mbt-utils]
    [docs.config.data :as config-data]
    [medley.core :as medley]))


(def config-docs-path (mbt-utils/safer-path "src" "doc" "docs" "pages" "config.md.tp"))

;;----------------------------------------------------------------------------------------------------------------------
;; fresh docs
;;----------------------------------------------------------------------------------------------------------------------
(defn new-line [] (emit! "\n"))


(defn emit-require []
  (emit! "◊(require '[docs.config.tags :refer [config-key]])◊")
  (new-line))


(defn fresh-tag [config-key]
  (format "◊config-key[%s]{}" (pr-str config-key)))


(defn emit-fresh-tag! [config-key]
  (emit! (fresh-tag config-key))
  (new-line))


(defn generate-doc-from-scratch []
  (compi/text-environment
    (emit-require) (new-line)
    (doseq [config-key config-data/config-keys]
      (emit-fresh-tag! config-key)
      (new-line))))


;;----------------------------------------------------------------------------------------------------------------------
;; Update existing
;;----------------------------------------------------------------------------------------------------------------------
(defn read-docs []
  (-> config-docs-path
      slurp
      textp-reader/read-from-string))


(defn collect-requires [forms]
  (take-while #(m/match %
                 ('require & ?_) true
                 (m/pred string?) true
                 ?_ false)
              forms))


(defn collect-written-docs [forms]
  (into {}
        (m/search forms
          (m/scan (m/and ?t
                         ('config-key
                           {:tag :tag-args-clj
                            :content [?config-key]}
                           & ?_)))

          (medley/map-entry ?config-key ?t))))


(defn updated-docs []
  (let [original (slurp config-docs-path)
        docs (textp-reader/read-from-string original)
        requires (collect-requires docs)
        already-written (collect-written-docs docs)]
    (compi/text-environment
      (doseq [r requires]
        (if (string? r)
          (emit! r)
          (emit! (textp-reader/form->text r original))))

      (doseq [config-key config-data/config-keys]
        (if-let [form (get already-written config-key)]
          (emit! (textp-reader/form->text form original))
          (emit-fresh-tag! config-key))
        (new-line)))))



(defn generate-doc-from-scratch! []
  (spit config-docs-path (generate-doc-from-scratch)))

(defn update-docs! []
  (spit config-docs-path (updated-docs)))

(comment
  ;(generate-doc-from-scratch!)
  (update-docs!))

