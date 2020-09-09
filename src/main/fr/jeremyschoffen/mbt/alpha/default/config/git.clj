(ns ^{:author "Jeremy Schoffen"
      :doc "
Default config pertaining to git utilities.
      "}
  fr.jeremyschoffen.mbt.alpha.default.config.git
  (:require
    [fr.jeremyschoffen.mbt.alpha.core :as mbt-core]
    [fr.jeremyschoffen.mbt.alpha.default.specs]
    [fr.jeremyschoffen.mbt.alpha.default.config.impl :as impl]
    [fr.jeremyschoffen.mbt.alpha.utils :as u]))

(u/pseudo-nss
  git
  project)

(defn git-repo
  "See [[fr.jeremyschoffen.mbt.alpha.core/git-make-jgit-repo]]."
  [param]
  (mbt-core/git-make-jgit-repo param))

(u/spec-op git-repo
           :deps [mbt-core/git-make-jgit-repo]
           :param {:req [::project/working-dir]}
           :ret ::git/repo)


(def conf {::git/repo (impl/calc git-repo ::project/working-dir)})
