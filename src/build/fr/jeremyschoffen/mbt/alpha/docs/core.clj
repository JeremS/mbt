(ns fr.jeremyschoffen.mbt.alpha.docs.core
  (:require
    [fr.jeremyschoffen.textp.alpha.doc.core :as doc]
    [fr.jeremyschoffen.mbt.alpha.utils :as u]
    [fr.jeremyschoffen.mbt.alpha.docs.config.compilation]
    [fr.jeremyschoffen.java.nio.alpha.file :as fs]))

(u/pseudo-nss
  project)

(def pages-dir (fs/path "fr/jeremyschoffen/mbt/alpha/docs/pages"))

(defn page-resource [s]
  (->> s
       (fs/path pages-dir)
       str))

(def readme-src (page-resource "README.md.tp"))

(defn make-readme! [{wd ::project/working-dir
                     coords ::project/maven-coords}]
  (spit (u/safer-path wd "README.md")
        (doc/make-document readme-src
                           {:project/maven-coords coords})))


(def rationale-src (page-resource "rationale.md.tp"))

(defn make-rationale! [{wd ::project/working-dir}]
  (spit (u/safer-path wd "doc" "rationale.md")
        (doc/make-document rationale-src {})))


(def design-src (page-resource "design.md.tp"))

(defn make-design-doc! [{wd ::project/working-dir}]
  (spit (u/safer-path wd "doc" "design.md")
        (doc/make-document design-src {})))


(def config-src (page-resource "config.md.tp"))

(defn make-config-doc! [{wd ::project/working-dir}]
  (spit (u/safer-path wd "doc" "config.md")
        (doc/make-document config-src {})))

(comment
  (doc/make-document
    config-src {})
  (ex-data *e)
  (-> config-src
      doc/slurp-resource)
      ;doc/read-document
      ;doc/eval-doc)

  'fr.jeremyschoffen.textp.alpha.html.tags
  'fr.jeremyschoffen.textp.alpha.html.compiler
  'fr.jeremyschoffen.textp.alpha.doc.tags)

