(ns completions)


;;----------------------------------------------------------------------------------------------------------------------
;; General
;;----------------------------------------------------------------------------------------------------------------------
:project/working-dir
:project/version
:project/author


;;----------------------------------------------------------------------------------------------------------------------
;; Licenses
;;----------------------------------------------------------------------------------------------------------------------
;license-distros
:repo:manual

:project.license/name
:project.license/url
:project.license/distribution
:project.license/comment
:project.license/file

:project/license

:project/licenses

;;----------------------------------------------------------------------------------------------------------------------
;; Deps
;;----------------------------------------------------------------------------------------------------------------------
:project.deps/file 
:project/deps 
:project.deps/aliases 


;;----------------------------------------------------------------------------------------------------------------------
;; Cleaning
;;----------------------------------------------------------------------------------------------------------------------
:cleaning/target

;;----------------------------------------------------------------------------------------------------------------------
;; Maven
;;----------------------------------------------------------------------------------------------------------------------
;; Basic maven
:maven/artefact-name 
:maven/group-id 
:maven/classifier 
:maven.pom/path
:maven.pom/xml
:maven/pom-properties 
:maven/local-repo 
:maven.settings/file 


;; Maven pom scm conf
:maven.scm/connection 
:maven.scm/developer-connection 
:maven.scm/tag 
:maven.scm/url 

:maven/scm 


;; Maven install conf
:maven.install/dir 


;; Maven credentials/auth conf
:maven.credentials/user-name 
:maven.credentials/password 
:maven.credentials/private-key 
:maven.credentials/passphrase 

:maven/credentials 

;; Maven Server conf
:maven.server/id 
:maven.server/url 
:maven/server 

;; Maven deployment conf
:maven.deploy.artefact/path 
:maven.deploy.artefact/extension 

:maven.deploy/artefact 

:maven.deploy/artefacts 


;;----------------------------------------------------------------------------------------------------------------------
;; Classpaths
;;----------------------------------------------------------------------------------------------------------------------
:classpath/nonexisting
:classpath/jar
:classpath/dir
:classpath/ext-dep
:classpath/file
:classpath/raw 
:classpath/raw-absolute 
:classpath/index 


;;----------------------------------------------------------------------------------------------------------------------
;; Jar
;;----------------------------------------------------------------------------------------------------------------------
:jar/main-ns 
:jar.manifest/overrides 
:jar/manifest 
:jar/output 
:jar/temp-output 
:jar/file-system 
:jar/exclude? 

:jar.entry/src 
:jar.entry/dest 
:jar/entry 

:jar/entries 

:jar.adding/result 
:jar.clash/strategy 


:jar/src 


:jar/srcs 


;;----------------------------------------------------------------------------------------------------------------------
;; Compilation
;;----------------------------------------------------------------------------------------------------------------------
;; Clojure
:compilation.clojure/namespaces 
:compilation.clojure/output-dir 


;; Java
:compilation.java/output-dir 
:compilation.java/sources 

:compilation.java/compiler 
:compilation.java/compiler-out 
:compilation.java/file-manager 
:compilation.java/diagnostic-listener 
:compilation.java/options 
:compilation.java/compiler-classes 
:compilation.java/compilation-unit 

:compilation.java.file-manager/diagnostic-listener 
:compilation.java.file-manager/locale 
:compilation.java.file-manager/charset 

:compilation.java.file-manager/options 


;;----------------------------------------------------------------------------------------------------------------------
;; Git
;;----------------------------------------------------------------------------------------------------------------------
:git/repo 
:git/top-level 
:git/prefix 


:git.add!/file-patterns 
:git.add!/update? 
:git.add!/working-tree-iterator

:git/add! 


:git.identity/name 
:git.identity/email 
:git/identity 

:git.commit/name 
:git.commit/message 
:git.commit/all? 
:git.commit/allow-empty? 
:git.commit/amend? 
:git.commit/author 
:git.commit/committer 
:git.commit/insert-change-id? 
:git.commit/no-verify? 
:git.commit/only 
:git.commit/reflog-comment 

:git/commit 

:git/commit! 

:git.tag/name 
:git.tag/message 
:git.tag/annotated? 
:git.tag/force? 
:git.tag/signed? 
:git.tag/tagger 

:git/tag 

:git/tag! 




:git.describe/tag-pattern 
:git.describe/distance 
:git/sha 
:git.repo/dirty? 


:git/raw-description 
:git/description 

:git/raw-description
:git/tag
:git/sha
:git.describe/distance
:git.repo/dirty?

;;----------------------------------------------------------------------------------------------------------------------
;; Shell
;;----------------------------------------------------------------------------------------------------------------------
:shell/command 

:shell/exit 
:shell/out 
:shell/err 

:shell/result 

;;----------------------------------------------------------------------------------------------------------------------
;; GPG
;;----------------------------------------------------------------------------------------------------------------------
:gpg/home-dir 
:gpg/command 
:gpg/version 
:gpg/key-id 
:gpg/pass-phrase 
:gpg.sign!/in 
:gpg.sign!/out 

:gpg/sign! 


;;----------------------------------------------------------------------------------------------------------------------
;; Versions
;;----------------------------------------------------------------------------------------------------------------------
:maven-like/subversions 

:alpha :beta :rc
:maven-like.qualifier/label 
:maven-like.qualifier/n 


:maven-like/qualifier 

:alpha :beta

:git-distance/number 
:git-distance/qualifier


;;----------------------------------------------------------------------------------------------------------------------
;; Defaults
;;----------------------------------------------------------------------------------------------------------------------
:project/name
:project/output-dir

;;----------------------------------------------------------------------------------------------------------------------
;; Jar building
;;----------------------------------------------------------------------------------------------------------------------
:build.jar/output-dir



:build/jar-name
:build/uberjar-name


;;----------------------------------------------------------------------------------------------------------------------
;; Deployment
;;----------------------------------------------------------------------------------------------------------------------
:maven.deploy/sign-artefacts?


;;----------------------------------------------------------------------------------------------------------------------
;; Versioning
;;----------------------------------------------------------------------------------------------------------------------
:versioning/bump-level?
:versioning/scheme
:versioning/tag-base-name

:versioning/version

:versioning/major

:version-file/path
:version-file/ns