(ns fr.jeremyschoffen.mbt.alpha.docs.config.compilation
  (:require
    [fr.jeremyschoffen.textp.alpha.lib.compilation :refer [emit!]]
    [fr.jeremyschoffen.textp.alpha.doc.markdown-compiler :as md]
    [fr.jeremyschoffen.textp.alpha.html.compiler :as html-compiler]))


(defmethod html-compiler/emit-tag! [::md/md :h2]
  [node]
  (emit! "\n")
  (emit! "## " (-> node :content first))
  (emit! "\n"))

(defmethod html-compiler/emit-tag! [::md/md :h3]
  [node]
  (emit! "\n")
  (emit! "### " (-> node :content first))
  (emit! "\n"))


(defmethod html-compiler/emit-tag! [::md/md :p]
  [node]
  (emit! "\n")
  (html-compiler/compile! (:content node))
  (emit! "\n"))


(defmethod html-compiler/emit-tag! [::md/md :ul]
  [node]
  (emit! "\n")
  (html-compiler/compile! (:content node)))


(defmethod html-compiler/emit-tag! [::md/md :li]
  [node]
  (emit! "- ") (html-compiler/compile! (:content node)) (emit! "\n"))


(defmethod html-compiler/emit-tag! [::md/md :docs.config.tags/config-key]
  [node]
  (html-compiler/compile! (:content node)))


(comment
  (require '[docs.config.tags])


  (def ex [{:tag :tag-args-clj
            :content [:fr.jeremyschoffen.mbt.alpha.versioning/version]}
           {:tag :tag-args-txt
            :content ["a description"]}])
  (println
    (md/doc->md (apply docs.config.tags/config-key ex))))