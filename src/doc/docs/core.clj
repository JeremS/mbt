(ns docs.core
  (:require
    [fr.jeremyschoffen.textp.alpha.doc.core :as doc]
    [fr.jeremyschoffen.mbt.alpha.utils :as u]))


(def readme-src "docs/pages/README.md.tp")

(defn make-readme! [{wd :project/working-dir
                     coords :project/maven-coords}]
  (spit (u/safer-path wd "README.md")
        (doc/make-document readme-src
                           {:project/maven-coords coords})))


(def rationale-src "docs/pages/rationale.md.tp")

(defn make-rationale! [{wd :project/working-dir}]
  (spit (u/safer-path wd "doc" "rationale.md")
        (doc/make-document rationale-src {})))


(def design-src "docs/pages/design.md.tp")

(defn make-design-doc! [{wd :project/working-dir}]
  (spit (u/safer-path wd "doc" "design.md")
        (doc/make-document design-src {})))

(comment
  (doc/make-document
    "docs/pages/README.md.tp" {})
  (ex-data *e)
  (-> "docs/pages/README.md.tp"
      doc/slurp-resource
      doc/read-document
      doc/eval-doc)

  'fr.jeremyschoffen.textp.alpha.html.tags
  'fr.jeremyschoffen.textp.alpha.html.compiler
  'fr.jeremyschoffen.textp.alpha.doc.tags)

