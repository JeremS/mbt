(ns com.jeremyschoffen.mbt.alpha.core.specs
  (:require
    [clojure.spec.alpha :as s]
    [clojure.tools.deps.alpha.specs :as deps-specs]
    [com.jeremyschoffen.java.nio.file :as fs])
  (:import (org.eclipse.jgit.api Git)))

(def path? fs/path?)
(def path-like? (some-fn path? string?))

(def file-system? fs/file-system?)

(def dir-path? (every-pred path? fs/directory?))
(def jar-path? (every-pred path?
                           #(= "jar" (fs/file-extention %))))


;;----------------------------------------------------------------------------------------------------------------------
;; General
;;----------------------------------------------------------------------------------------------------------------------
(s/def :project/working-dir (every-pred path-like? fs/absolute?))
(s/def :project/version string?)
(s/def :project/author string?)


;;----------------------------------------------------------------------------------------------------------------------
;; Cleaning
;;----------------------------------------------------------------------------------------------------------------------
(s/def :cleaning/target path?)

;;----------------------------------------------------------------------------------------------------------------------
;; Maven
;;----------------------------------------------------------------------------------------------------------------------
;; Basic maven
(s/def :maven/artefact-name symbol?)
(s/def :maven/group-id symbol?)
(s/def :maven/classifier symbol?)
(s/def :maven.pom/dir path?)
(s/def :maven/pom map?)
(s/def :maven/local-repo path?)
(s/def :maven.settings/file path?)

;; Maven credentials/auth conf
(s/def :maven.credentials/user-name string?)
(s/def :maven.credentials/password string?)
(s/def :maven.credentials/private-key path?)
(s/def :maven.credentials/passphrase string?)

(s/def :maven/credentials (s/keys :opt [:maven.credentials/user-name
                                        :maven.credentials/password
                                        :maven.credentials/private-key
                                        :maven.credentials/passphrase]))

;; Maven Server conf
(s/def :maven.server/id string?)
(s/def :maven.server/url fs/url?)
(s/def :maven/server (s/keys :opt [:maven.server/id
                                   :maven.server/url]))

;; Maven deployment conf
(s/def :maven.deploy.artefact/path path?)
(s/def :maven.deploy.artefact/extension string?)

(s/def :maven.deploy/artefact
  (s/keys :req [:maven.deploy.artefact/path
                :maven.deploy.artefact/extension]))

(s/def :maven.deploy/artefacts (s/coll-of :maven.deploy/artefact))


;;----------------------------------------------------------------------------------------------------------------------
;; Deps
;;----------------------------------------------------------------------------------------------------------------------
(s/def :project/deps ::deps-specs/deps-map)
(s/def :project.deps/aliases (s/coll-of keyword? :into #{}))

;;----------------------------------------------------------------------------------------------------------------------
;; Classpaths
;;----------------------------------------------------------------------------------------------------------------------
(def classpath-index-categories #{:classpath/nonexisting
                                  :classpath/jar
                                  :classpath/dir
                                  :classpath/ext-dep
                                  :classpath/file})
(s/def :classpath/raw string?)
(s/def :classpath/index (s/map-of classpath-index-categories
                                  (s/coll-of string?)))


;;----------------------------------------------------------------------------------------------------------------------
;; Jar
;;----------------------------------------------------------------------------------------------------------------------
(s/def :jar/main-ns symbol?)
(s/def :jar.manifest/overrides map?)
(s/def :jar/manifest string?)
(s/def :jar/output jar-path?)
(s/def :jar/temp-output path?)
(s/def :jar/file-system file-system?)
(s/def :jar/exclude? fn?)

(s/def :jar.entry/src (s/or :text string? :file path?))
(s/def :jar.entry/dest path?)
(s/def :jar/entry (s/keys :req [:jar.entry/src :jar.entry/dest]))

(s/def :jar/entries (s/coll-of :jar/entry))

(s/def :jar.adding/result any?)
(s/def :jar.clash/strategy #{:merge :concat-lines :noop})


(s/def :jar/src (s/or :path path?
                      :entries (s/coll-of :jar/entry)))
(s/def :jar/srcs (s/coll-of :jar/src))


;;----------------------------------------------------------------------------------------------------------------------
;; Compilation
;;----------------------------------------------------------------------------------------------------------------------
(s/def :clojure.compilation/namespaces (s/coll-of symbol?))
(s/def :clojure.compilation/output-dir path?)


;;----------------------------------------------------------------------------------------------------------------------
;; Git
;;----------------------------------------------------------------------------------------------------------------------
(s/def :git/repo #(isa? (type %) Git))
(s/def :git/top-level fs/path?)
(s/def :git/prefix (every-pred fs/path? (complement fs/absolute?)))


(s/def :git.addition/file-patterns (s/coll-of string?))
(s/def :git.addition/update? boolean?)
(s/def :git.addition/working-tree-iterator any?) ;; TODO: find the right type

(s/def :git/addition (s/keys :req [:git.addition/file-patterns]
                             :opt [:git.addition/update?
                                   :git.addition/working-tree-iterator]))


(s/def :git.identity/name string?)
(s/def :git.identity/email string?)
(s/def :git/identity (s/keys :req [:git.identity/name :git.identity/email]))

(s/def :git.commit/name string?)
(s/def :git.commit/message string?)
(s/def :git.commit/all? boolean?)
(s/def :git.commit/allow-empty? boolean?)
(s/def :git.commit/amend? boolean?)
(s/def :git.commit/author :git/identity)
(s/def :git.commit/committer :git/identity)
(s/def :git.commit/insert-change-id? boolean?)
(s/def :git.commit/no-verify? boolean?)
(s/def :git.commit/only string?)
(s/def :git.commit/reflog-comment string?)

(s/def :git/commit (s/keys :req [:git.commit/name
                                 :git.commit/message]
                           :opt [:git.commit/author
                                 :git.commit/committer
                                 :git.commit/reflog-comment]))

(s/def :git/commit! (s/keys :req [:git.commit/message]
                            :opt [:git.commit/all?
                                  :git.commit/allow-empty?
                                  :git.commit/amend?
                                  :git.commit/author
                                  :git.commit/committer
                                  :git.commit/insert-change-id?
                                  :git.commit/no-verify?
                                  :git.commit/only
                                  :git.commit/reflog-comment]))


(s/def :git.tag/name string?)
(s/def :git.tag/message string?)
(s/def :git.tag/annotated? boolean?)
(s/def :git.tag/force? boolean?)
(s/def :git.tag/signed? boolean?)
(s/def :git.tag/tagger :git/identity)

(s/def :git/tag (s/keys :req [:git.tag/name
                              :git.tag/message]
                        :opt [:git.tag/tagger]))

(s/def :git/tag! (s/keys :req [:git.tag/name
                               :git.tag/message]
                         :opt [:git.tag/annotated?
                               :git.tag/force?
                               :git.tag/signed?
                               :git.tag/tagger]))




(s/def :git.describe/tag-pattern string?)
(s/def :git.describe/distance int?)
(s/def :git/sha string?)
(s/def :git.repo/dirty? boolean?)


(s/def :git/raw-description string?)
(s/def :git/description
  (s/keys :req [:git/raw-description
                :git/tag
                :git/sha
                :git.describe/distance
                :git.repo/dirty?]))

(def description-keys #{:git/raw-description
                        :git/tag
                        :git/sha
                        :git.describe/distance
                        :git.repo/dirty?})

;;----------------------------------------------------------------------------------------------------------------------
;; Shell
;;----------------------------------------------------------------------------------------------------------------------
(s/def :shell/command (s/* string?))


;;----------------------------------------------------------------------------------------------------------------------
;; GPG
;;----------------------------------------------------------------------------------------------------------------------
(s/def :gpg.sign/in path?)
(s/def :gpg.sign/out path?)
(s/def :gpg/key-id string?)

(s/def :gpg.sign/spec (s/keys :req [:gpg.sign/in]
                              :opt [:gpg.sign/out
                                    :gpg/key-id]))

(s/def :gpg.sign/specs (s/* :gpg.sign/spec))


;;----------------------------------------------------------------------------------------------------------------------
;; Versions
;;----------------------------------------------------------------------------------------------------------------------
(s/def :maven-like/subversions (s/coll-of integer? :kind vector? :count 3))

(def allowed-qualifiers #{:alpha :beta :rc})
(s/def :maven-like.qualifier/label allowed-qualifiers)
(s/def :maven-like.qualifier/n (s/and integer? pos?))


(s/def :maven-like/qualifier (s/keys :req-un [:maven-like.qualifier/label
                                              :maven-like.qualifier/n]))

(s/def :simple-version/number integer?)
