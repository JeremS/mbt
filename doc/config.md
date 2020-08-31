

# Config keys reference


##  `:fr.jeremyschoffen.mbt.alpha.build/jar-output-dir`

### Spec:
```clojure
path?
```
### Description:


The dir into which build jars.


### Constructors:

- [[fr.jeremyschoffen.mbt.alpha.default.config.build/jar-out-dir]]


##  `:fr.jeremyschoffen.mbt.alpha.build.jar/allow-non-maven-deps`

### Spec:
```clojure
boolean?
```
### Description:


Config option for the default api when it comes to producing skinny jars.
Defaulting to false, [[fr.jeremyschoffen.mbt.alpha.default/build-jar!]] will throw
an exception if there are non *maven compatible* deps used in the project.

The idea is to guard from shipping a jar in which the pom.xml can't provide all deps.




##  `:fr.jeremyschoffen.mbt.alpha.build.jar/name`

### Spec:
```clojure
(every-pred string? jar-ext?)
```
### Description:


The file name of the skinny jar to build.


### Constructors:

- [[fr.jeremyschoffen.mbt.alpha.default.config.build/jar-name]]


##  `:fr.jeremyschoffen.mbt.alpha.build.jar/path`

### Spec:
```clojure
jar-path?
```
### Description:


The definitive location of the jar to build : jar.output/dir + jar/name


### Constructors:

- [[fr.jeremyschoffen.mbt.alpha.default.config.build/jar-out]]


##  `:fr.jeremyschoffen.mbt.alpha.build.uberjar/name`

### Spec:
```clojure
(every-pred string? jar-ext?)
```
### Description:


The file name of the uberjar to build.


### Constructors:

- [[fr.jeremyschoffen.mbt.alpha.default.config.build/uberjar-name]]


##  `:fr.jeremyschoffen.mbt.alpha.build.uberjar/path`

### Spec:
```clojure
jar-path?
```
### Description:


The definitive location of the jar to build : jar.output/dir + uberjar/name


### Constructors:

- [[fr.jeremyschoffen.mbt.alpha.default.config.build/uberjar-out]]


##  `:fr.jeremyschoffen.mbt.alpha.classpath/index`

### Spec:
```clojure
(map-of classpath-index-categories (coll-of string?))
```
### Description:


An indexed classpath used to derive jar sources.


### Constructors:

- [[fr.jeremyschoffen.mbt.alpha.core/classpath-indexed]]
- [[fr.jeremyschoffen.mbt.alpha.core.classpath/indexed-classpath]]


##  `:fr.jeremyschoffen.mbt.alpha.classpath/raw`

### Spec:
```clojure
string?
```
### Description:


A classpath string.


### Constructors:

- [[fr.jeremyschoffen.mbt.alpha.core/classpath-raw]]
- [[fr.jeremyschoffen.mbt.alpha.core.classpath/raw-classpath]]


##  `:fr.jeremyschoffen.mbt.alpha.cleaning/target`

### Spec:
```clojure
path?
```
### Description:


Path to a file / directory to delete.


### Constructors:

- [[fr.jeremyschoffen.mbt.alpha.default.config.cleaning/cleaning-target]]


##  `:fr.jeremyschoffen.mbt.alpha.compilation.clojure/namespaces`

### Spec:
```clojure
(coll-of symbol?)
```
### Description:


List of namespaces of clojure namespaces to compile.


### Constructors:

- [[fr.jeremyschoffen.mbt.alpha.core/compilation-clojure-external-nss]]
- [[fr.jeremyschoffen.mbt.alpha.core/compilation-clojure-jar-nss]]
- [[fr.jeremyschoffen.mbt.alpha.core/compilation-clojure-project-nss]]
- [[fr.jeremyschoffen.mbt.alpha.core.compilation.clojure/external-nss]]
- [[fr.jeremyschoffen.mbt.alpha.core.compilation.clojure/jar-nss]]
- [[fr.jeremyschoffen.mbt.alpha.core.compilation.clojure/project-nss]]


##  `:fr.jeremyschoffen.mbt.alpha.compilation.clojure/output-dir`

### Spec:
```clojure
path?
```
### Description:


Directory where clojure copilation will output.


### Constructors:

- [[fr.jeremyschoffen.mbt.alpha.default.config.compilation/compilation-clojure-dir]]


##  `:fr.jeremyschoffen.mbt.alpha.compilation.java/compilation-unit`

### Spec:
```clojure
(instance? java.lang.Iterable %)
```
### Description:


A java compilation unit required by the java api. Contains the paths to
the .java files to compile.



##  `:fr.jeremyschoffen.mbt.alpha.compilation.java/compiler`

### Spec:
```clojure
(instance? javax.tools.JavaCompiler %)
```
### Constructors:

- [[fr.jeremyschoffen.mbt.alpha.core/compilation-java-compiler]]
- [[fr.jeremyschoffen.mbt.alpha.core.compilation.java/make-java-compiler]]


##  `:fr.jeremyschoffen.mbt.alpha.compilation.java/compiler-classes`

### Spec:
```clojure
(coll-of string? :kind vector?)
```

##  `:fr.jeremyschoffen.mbt.alpha.compilation.java/compiler-out`

### Spec:
```clojure
(instance? java.io.Writer %)
```

##  `:fr.jeremyschoffen.mbt.alpha.compilation.java/diagnostic-listener`

### Spec:
```clojure
(instance? javax.tools.DiagnosticListener %)
```

##  `:fr.jeremyschoffen.mbt.alpha.compilation.java/file-manager`

### Spec:
```clojure
(instance? javax.tools.StandardJavaFileManager %)
```

##  `:fr.jeremyschoffen.mbt.alpha.compilation.java/options`

### Spec:
```clojure
(coll-of string? :kind vector?)
```

##  `:fr.jeremyschoffen.mbt.alpha.compilation.java/output-dir`

### Spec:
```clojure
path?
```
### Constructors:

- [[fr.jeremyschoffen.mbt.alpha.default.config.compilation/compilation-java-dir]]


##  `:fr.jeremyschoffen.mbt.alpha.compilation.java/sources`

### Spec:
```clojure
(coll-of path?)
```
### Constructors:

- [[fr.jeremyschoffen.mbt.alpha.core/compilation-java-external-files]]
- [[fr.jeremyschoffen.mbt.alpha.core/compilation-java-jar-files]]
- [[fr.jeremyschoffen.mbt.alpha.core/compilation-java-project-files]]
- [[fr.jeremyschoffen.mbt.alpha.core.compilation.java/external-files]]
- [[fr.jeremyschoffen.mbt.alpha.core.compilation.java/jar-files]]
- [[fr.jeremyschoffen.mbt.alpha.core.compilation.java/project-files]]


##  `:fr.jeremyschoffen.mbt.alpha.compilation.java.file-manager/charset`

### Spec:
```clojure
(instance? java.nio.charset.Charset %)
```

##  `:fr.jeremyschoffen.mbt.alpha.compilation.java.file-manager/diagnostic-listener`

### Spec:
```clojure
(instance? javax.tools.DiagnosticListener %)
```

##  `:fr.jeremyschoffen.mbt.alpha.compilation.java.file-manager/locale`

### Spec:
```clojure
(instance? java.util.Locale %)
```

##  `:fr.jeremyschoffen.mbt.alpha.compilation.java.file-manager/options`

### Spec:
```clojure
(keys
 :opt
 [:fr.jeremyschoffen.mbt.alpha.compilation.java.file-manager/diagnostic-listener
  :fr.jeremyschoffen.mbt.alpha.compilation.java.file-manager/locale
  :fr.jeremyschoffen.mbt.alpha.compilation.java.file-manager/charset])
```

##  `:fr.jeremyschoffen.mbt.alpha.git/add!`

### Spec:
```clojure
(keys
 :req
 [:fr.jeremyschoffen.mbt.alpha.git.add!/file-patterns]
 :opt
 [:fr.jeremyschoffen.mbt.alpha.git.add!/update?
  :fr.jeremyschoffen.mbt.alpha.git.add!/working-tree-iterator])
```
### Description:


Options used when staging files in git.



##  `:fr.jeremyschoffen.mbt.alpha.git/commit`

### Spec:
```clojure
(keys
 :req
 [:fr.jeremyschoffen.mbt.alpha.git.commit/name
  :fr.jeremyschoffen.mbt.alpha.git.commit/message]
 :opt
 [:fr.jeremyschoffen.mbt.alpha.git.commit/author
  :fr.jeremyschoffen.mbt.alpha.git.commit/committer
  :fr.jeremyschoffen.mbt.alpha.git.commit/reflog-comment])
```
### Description:


Data found in a git commit.



##  `:fr.jeremyschoffen.mbt.alpha.git/commit!`

### Spec:
```clojure
(keys
 :req
 [:fr.jeremyschoffen.mbt.alpha.git.commit/message]
 :opt
 [:fr.jeremyschoffen.mbt.alpha.git.commit/all?
  :fr.jeremyschoffen.mbt.alpha.git.commit/allow-empty?
  :fr.jeremyschoffen.mbt.alpha.git.commit/amend?
  :fr.jeremyschoffen.mbt.alpha.git.commit/author
  :fr.jeremyschoffen.mbt.alpha.git.commit/committer
  :fr.jeremyschoffen.mbt.alpha.git.commit/insert-change-id?
  :fr.jeremyschoffen.mbt.alpha.git.commit/no-verify?
  :fr.jeremyschoffen.mbt.alpha.git.commit/only
  :fr.jeremyschoffen.mbt.alpha.git.commit/reflog-comment])
```
### Description:


Options used when commiting to git.



##  `:fr.jeremyschoffen.mbt.alpha.git/description`

### Spec:
```clojure
(keys
 :req
 [:fr.jeremyschoffen.mbt.alpha.git/raw-description
  :fr.jeremyschoffen.mbt.alpha.git/tag
  :fr.jeremyschoffen.mbt.alpha.git/sha
  :fr.jeremyschoffen.mbt.alpha.git.describe/distance
  :fr.jeremyschoffen.mbt.alpha.git.repo/dirty?])
```
### Description:


Data found in a git description.



##  `:fr.jeremyschoffen.mbt.alpha.git/identity`

### Spec:
```clojure
(keys
 :req
 [:fr.jeremyschoffen.mbt.alpha.git.identity/name
  :fr.jeremyschoffen.mbt.alpha.git.identity/email])
```
### Description:


Data representing an identity in git (committer...).



##  `:fr.jeremyschoffen.mbt.alpha.git/prefix`

### Spec:
```clojure
(every-pred path? (complement absolute?))
```
### Description:


Git prefix as in `git -C wd/ rev-parse --show-prefix`


### Constructors:

- [[fr.jeremyschoffen.mbt.alpha.core/git-prefix]]
- [[fr.jeremyschoffen.mbt.alpha.core.git/prefix]]


##  `:fr.jeremyschoffen.mbt.alpha.git/raw-description`

### Spec:
```clojure
string?
```
### Description:


The string returned by `git describe -opts*` given the options we pass by default.



##  `:fr.jeremyschoffen.mbt.alpha.git/repo`

### Spec:
```clojure
(isa? (type %) org.eclipse.jgit.api.Git)
```
### Description:


A JGit object representing a git repo.


### Constructors:

- [[fr.jeremyschoffen.mbt.alpha.core/git-make-jgit-repo]]
- [[fr.jeremyschoffen.mbt.alpha.core.git/make-jgit-repo]]
- [[fr.jeremyschoffen.mbt.alpha.default.config.git/git-repo]]


##  `:fr.jeremyschoffen.mbt.alpha.git/sha`

### Spec:
```clojure
string?
```
### Description:


A git sha.



##  `:fr.jeremyschoffen.mbt.alpha.git/tag`

### Spec:
```clojure
(keys
 :req
 [:fr.jeremyschoffen.mbt.alpha.git.tag/name
  :fr.jeremyschoffen.mbt.alpha.git.tag/message]
 :opt
 [:fr.jeremyschoffen.mbt.alpha.git.tag/tagger])
```
### Description:


Clojure data representing a git tag.


### Constructors:

- [[fr.jeremyschoffen.mbt.alpha.core/git-git-get-tag]]
- [[fr.jeremyschoffen.mbt.alpha.core/git-tag!]]
- [[fr.jeremyschoffen.mbt.alpha.core.git/get-tag]]
- [[fr.jeremyschoffen.mbt.alpha.core.git/tag!]]
- [[fr.jeremyschoffen.mbt.alpha.default/versioning-tag-new-version!]]
- [[fr.jeremyschoffen.mbt.alpha.default.versioning/tag-new-version!]]
- [[fr.jeremyschoffen.mbt.alpha.default.versioning.git-state/new-tag]]
- [[fr.jeremyschoffen.mbt.alpha.default.versioning.git-state/tag-new-version!]]


##  `:fr.jeremyschoffen.mbt.alpha.git/tag!`

### Spec:
```clojure
(keys
 :req
 [:fr.jeremyschoffen.mbt.alpha.git.tag/name
  :fr.jeremyschoffen.mbt.alpha.git.tag/message]
 :opt
 [:fr.jeremyschoffen.mbt.alpha.git.tag/annotated?
  :fr.jeremyschoffen.mbt.alpha.git.tag/force?
  :fr.jeremyschoffen.mbt.alpha.git.tag/signed?
  :fr.jeremyschoffen.mbt.alpha.git.tag/tagger])
```
### Description:


Option used when creating a new git tag.



##  `:fr.jeremyschoffen.mbt.alpha.git/top-level`

### Spec:
```clojure
path?
```
### Description:


Git top level as in `git -C wd/ rev-parse --show-toplevel`.


### Constructors:

- [[fr.jeremyschoffen.mbt.alpha.core/git-top-level]]
- [[fr.jeremyschoffen.mbt.alpha.core.git/top-level]]


##  `:fr.jeremyschoffen.mbt.alpha.git-distance/number`

### Spec:
```clojure
integer?
```
### Description:


Number representing a distance from an earlier commit. We get it from git descriptions.
It's used when computing version numbers.



##  `:fr.jeremyschoffen.mbt.alpha.git-distance/qualifier`

### Spec:
```clojure
git-distance-qualifiers
```
### Description:


Qualifier for a version nulber in the `git-distance scheme`.



##  `:fr.jeremyschoffen.mbt.alpha.git.add!/file-patterns`

### Spec:
```clojure
(coll-of string?)
```
### Description:


Pattern to be passed to git add.



##  `:fr.jeremyschoffen.mbt.alpha.git.add!/update?`

### Spec:
```clojure
boolean?
```
### Description:


Git add option.



##  `:fr.jeremyschoffen.mbt.alpha.git.add!/working-tree-iterator`

### Spec:
```clojure
any?
```
### Description:


Git add option.



##  `:fr.jeremyschoffen.mbt.alpha.git.commit/all?`

### Spec:
```clojure
boolean?
```
### Description:


Git commit option.



##  `:fr.jeremyschoffen.mbt.alpha.git.commit/allow-empty?`

### Spec:
```clojure
boolean?
```
### Description:


Git commit option.



##  `:fr.jeremyschoffen.mbt.alpha.git.commit/amend?`

### Spec:
```clojure
boolean?
```
### Description:


Git commit option.



##  `:fr.jeremyschoffen.mbt.alpha.git.commit/author`

### Spec:
```clojure
(keys
 :req
 [:fr.jeremyschoffen.mbt.alpha.git.identity/name
  :fr.jeremyschoffen.mbt.alpha.git.identity/email])
```
### Description:


Git commit option.



##  `:fr.jeremyschoffen.mbt.alpha.git.commit/committer`

### Spec:
```clojure
(keys
 :req
 [:fr.jeremyschoffen.mbt.alpha.git.identity/name
  :fr.jeremyschoffen.mbt.alpha.git.identity/email])
```
### Description:


Git commit option.



##  `:fr.jeremyschoffen.mbt.alpha.git.commit/insert-change-id?`

### Spec:
```clojure
boolean?
```
### Description:


Git commit option.



##  `:fr.jeremyschoffen.mbt.alpha.git.commit/message`

### Spec:
```clojure
string?
```
### Description:


Message in a commit.



##  `:fr.jeremyschoffen.mbt.alpha.git.commit/name`

### Spec:
```clojure
string?
```
### Description:


Name of a commit.



##  `:fr.jeremyschoffen.mbt.alpha.git.commit/no-verify?`

### Spec:
```clojure
boolean?
```
### Description:


Git commit option.



##  `:fr.jeremyschoffen.mbt.alpha.git.commit/only`

### Spec:
```clojure
string?
```
### Description:


Git commit option.



##  `:fr.jeremyschoffen.mbt.alpha.git.commit/reflog-comment`

### Spec:
```clojure
string?
```
### Description:


Git commit option.



##  `:fr.jeremyschoffen.mbt.alpha.git.describe/distance`

### Spec:
```clojure
int?
```
### Description:


Git distance in a description.



##  `:fr.jeremyschoffen.mbt.alpha.git.describe/tag-pattern`

### Spec:
```clojure
string?
```
### Description:


The pattern used to find a previous commit in a description.



##  `:fr.jeremyschoffen.mbt.alpha.git.identity/email`

### Spec:
```clojure
string?
```
### Description:


Email in a git identity.



##  `:fr.jeremyschoffen.mbt.alpha.git.identity/name`

### Spec:
```clojure
string?
```
### Description:


Name in a git identity.



##  `:fr.jeremyschoffen.mbt.alpha.git.repo/dirty?`

### Spec:
```clojure
boolean?
```
### Description:


Whether the repo is dirty in a description.



##  `:fr.jeremyschoffen.mbt.alpha.git.tag/annotated?`

### Spec:
```clojure
boolean?
```
### Description:


  Git tag option.



##  `:fr.jeremyschoffen.mbt.alpha.git.tag/force?`

### Spec:
```clojure
boolean?
```
### Description:


Git tag option.



##  `:fr.jeremyschoffen.mbt.alpha.git.tag/message`

### Spec:
```clojure
string?
```
### Description:


Message of a tag.



##  `:fr.jeremyschoffen.mbt.alpha.git.tag/name`

### Spec:
```clojure
string?
```
### Description:


Name of a tag.



##  `:fr.jeremyschoffen.mbt.alpha.git.tag/signed?`

### Spec:
```clojure
boolean?
```
### Description:


Git tag option.



##  `:fr.jeremyschoffen.mbt.alpha.git.tag/tagger`

### Spec:
```clojure
(keys
 :req
 [:fr.jeremyschoffen.mbt.alpha.git.identity/name
  :fr.jeremyschoffen.mbt.alpha.git.identity/email])
```
### Description:


Identity of the tagger.



##  `:fr.jeremyschoffen.mbt.alpha.gpg/command`

### Spec:
```clojure
string?
```
### Description:


The gpg command to use at the command line. Typically `gpg` or `gpg2`.



##  `:fr.jeremyschoffen.mbt.alpha.gpg/home-dir`

### Spec:
```clojure
path?
```
### Description:


The `gpg home directory` to point gpg to.



##  `:fr.jeremyschoffen.mbt.alpha.gpg/key-id`

### Spec:
```clojure
string?
```
### Description:


Id of the key gpg should use to sign artefacts.



##  `:fr.jeremyschoffen.mbt.alpha.gpg/pass-phrase`

### Spec:
```clojure
string?
```
### Description:


A passphrase for the specified / default key gpg will use to sign artefacts.



##  `:fr.jeremyschoffen.mbt.alpha.gpg/sign!`

### Spec:
```clojure
(keys
 :req
 [:fr.jeremyschoffen.mbt.alpha.gpg.sign!/in]
 :opt
 [:fr.jeremyschoffen.mbt.alpha.gpg.sign!/out])
```
### Description:


Signing options for gpg:
- what to sign.
- where to put the signature.



##  `:fr.jeremyschoffen.mbt.alpha.gpg/version`

### Spec:
```clojure
(and vector? (cat :major int? :minor int? :patch int?))
```
### Description:


Internal representation of gpg's version used to dictate what options
are required to use when signing.


### Constructors:

- [[fr.jeremyschoffen.mbt.alpha.core/gpg-version]]
- [[fr.jeremyschoffen.mbt.alpha.core.gpg/gpg-version]]


##  `:fr.jeremyschoffen.mbt.alpha.gpg.sign!/in`

### Spec:
```clojure
path?
```
### Description:


File to sign with gpg.



##  `:fr.jeremyschoffen.mbt.alpha.gpg.sign!/out`

### Spec:
```clojure
path?
```
### Description:


Where the signature gpg generates goes.


### Constructors:

- [[fr.jeremyschoffen.mbt.alpha.core.gpg/make-sign-out]]


##  `:fr.jeremyschoffen.mbt.alpha.jar/entries`

### Spec:
```clojure
(coll-of :fr.jeremyschoffen.mbt.alpha.jar/entry)
```
### Description:


A collection of jar entries.


### Constructors:

- [[fr.jeremyschoffen.mbt.alpha.core/jar-add-srcs!]]
- [[fr.jeremyschoffen.mbt.alpha.core.jar/add-srcs!]]
- [[fr.jeremyschoffen.mbt.alpha.core.jar.temp/add-srcs!]]


##  `:fr.jeremyschoffen.mbt.alpha.jar/entry`

### Spec:
```clojure
(keys
 :req
 [:fr.jeremyschoffen.mbt.alpha.jar.entry/src
  :fr.jeremyschoffen.mbt.alpha.jar.entry/dest])
```
### Description:


Representation of a jar entry.


### Constructors:

- [[fr.jeremyschoffen.mbt.alpha.default.jar/make-manifest-entry]]


##  `:fr.jeremyschoffen.mbt.alpha.jar/exclude?`

### Spec:
```clojure
fn?
```
### Description:


Function determining if a potential jar entry actually makes it into a jar.



##  `:fr.jeremyschoffen.mbt.alpha.jar/file-system`

### Spec:
```clojure
file-system?
```
### Description:


A java nio filesystem pointing at a jar.


### Constructors:

- [[fr.jeremyschoffen.mbt.alpha.core.jar/writable-jar-fs]]
- [[fr.jeremyschoffen.mbt.alpha.core.jar.fs/writable-jar-fs]]


##  `:fr.jeremyschoffen.mbt.alpha.jar/main-ns`

### Spec:
```clojure
symbol?
```
### Description:


the main namespace of a jar. (Used in jar manifests.)



##  `:fr.jeremyschoffen.mbt.alpha.jar/manifest`

### Spec:
```clojure
string?
```
### Description:


A jar manifest.


### Constructors:

- [[fr.jeremyschoffen.mbt.alpha.core/manifest]]
- [[fr.jeremyschoffen.mbt.alpha.core.jar.manifest/make-manifest]]


##  `:fr.jeremyschoffen.mbt.alpha.jar/output`

### Spec:
```clojure
jar-path?
```
### Description:


The path poiting at the location a jar will be created.



##  `:fr.jeremyschoffen.mbt.alpha.jar/src`

### Spec:
```clojure
(satisfies? JarSource %)
```
### Description:


Source for a jar: source directories, other jars...


### Constructors:

- [[fr.jeremyschoffen.mbt.alpha.default.jar/make-staples-entries]]


##  `:fr.jeremyschoffen.mbt.alpha.jar/srcs`

### Spec:
```clojure
(coll-of :fr.jeremyschoffen.mbt.alpha.jar/src)
```
### Description:


Collection aof jar sources.


### Constructors:

- [[fr.jeremyschoffen.mbt.alpha.default.jar/simple-jar-srcs]]
- [[fr.jeremyschoffen.mbt.alpha.default.jar/uber-jar-srcs]]


##  `:fr.jeremyschoffen.mbt.alpha.jar/temp-output`

### Spec:
```clojure
path?
```
### Description:


The location of the directory containing all jar entries. Basically the un-compressed
content of a jar archive to be compressed.



##  `:fr.jeremyschoffen.mbt.alpha.jar.adding/result`

### Spec:
```clojure
any?
```
### Description:


Key found in the returned values of the jar-ing operation giving some information
as to what happened to the jar entry.



##  `:fr.jeremyschoffen.mbt.alpha.jar.clash/strategy`

### Spec:
```clojure
#{:noop :concat-lines :merge}
```
### Description:


Key found in the returned values of the jar-ing operation indicating a clash
happened and what strategy was employed to resolve it.



##  `:fr.jeremyschoffen.mbt.alpha.jar.entry/dest`

### Spec:
```clojure
path?
```
### Description:


The destination of a jar entry in the final jar.



##  `:fr.jeremyschoffen.mbt.alpha.jar.entry/src`

### Spec:
```clojure
(or :text string? :file path?)
```
### Description:


The path to the actual jar entry.



##  `:fr.jeremyschoffen.mbt.alpha.jar.manifest/overrides`

### Spec:
```clojure
map?
```
### Description:


Additionnal entries for the a jar manifest.



##  `:fr.jeremyschoffen.mbt.alpha.maven/artefact-name`

### Spec:
```clojure
symbol?
```
### Description:


The name of a maven artefact (devoid of classifier).


### Constructors:

- [[fr.jeremyschoffen.mbt.alpha.default.config.maven/artefact-name]]


##  `:fr.jeremyschoffen.mbt.alpha.maven/classifier`

### Spec:
```clojure
symbol?
```
### Description:


A classifer to be employed for a mavan artefact.



##  `:fr.jeremyschoffen.mbt.alpha.maven/credentials`

### Spec:
```clojure
(keys
 :opt
 [:fr.jeremyschoffen.mbt.alpha.maven.credentials/user-name
  :fr.jeremyschoffen.mbt.alpha.maven.credentials/password
  :fr.jeremyschoffen.mbt.alpha.maven.credentials/private-key
  :fr.jeremyschoffen.mbt.alpha.maven.credentials/passphrase])
```
### Description:


Credentials used in maven operations.



##  `:fr.jeremyschoffen.mbt.alpha.maven/group-id`

### Spec:
```clojure
symbol?
```
### Description:


The group id for a maven artefact.


### Constructors:

- [[fr.jeremyschoffen.mbt.alpha.default.config.maven/group-id]]


##  `:fr.jeremyschoffen.mbt.alpha.maven/local-repo`

### Spec:
```clojure
path?
```
### Description:


Path to the local maven repo.


### Constructors:

- [[fr.jeremyschoffen.mbt.alpha.default.config.maven/maven-local-repo]]


##  `:fr.jeremyschoffen.mbt.alpha.maven/scm`

### Spec:
```clojure
(keys
 :opt
 [:fr.jeremyschoffen.mbt.alpha.maven.scm/connection
  :fr.jeremyschoffen.mbt.alpha.maven.scm/developer-connection
  :fr.jeremyschoffen.mbt.alpha.maven.scm/tag
  :fr.jeremyschoffen.mbt.alpha.maven.scm/url])
```
### Description:


Scm information for pom.xml files.



##  `:fr.jeremyschoffen.mbt.alpha.maven/server`

### Spec:
```clojure
(keys
 :opt
 [:fr.jeremyschoffen.mbt.alpha.maven.server/id
  :fr.jeremyschoffen.mbt.alpha.maven.server/url])
```
### Description:


Server information when deploying maven artefacts.



##  `:fr.jeremyschoffen.mbt.alpha.maven-like/qualifier`

### Spec:
```clojure
(keys
 :req-un
 [:fr.jeremyschoffen.mbt.alpha.maven-like.qualifier/label
  :fr.jeremyschoffen.mbt.alpha.maven-like.qualifier/n])
```
### Description:


qualifier in the maven versioning: `alpha3`, `rc2`...



##  `:fr.jeremyschoffen.mbt.alpha.maven-like/subversions`

### Spec:
```clojure
(coll-of integer? :kind vector? :count 3)
```
### Description:


Vector of 3 integers used to describe a maven / semver version number (major, minor, patch).



##  `:fr.jeremyschoffen.mbt.alpha.maven-like.qualifier/label`

### Spec:
```clojure
allowed-qualifiers
```
### Description:


Name of the qualifier in the maven versioning scheme: `:alpha`, `:beta`, `:rc`.



##  `:fr.jeremyschoffen.mbt.alpha.maven-like.qualifier/n`

### Spec:
```clojure
(and integer? pos?)
```
### Description:


The number in maven version qualifier: the 2 in `alpha2`.



##  `:fr.jeremyschoffen.mbt.alpha.maven.credentials/passphrase`

### Spec:
```clojure
string?
```
### Description:


Passphrase used in maven credentials.



##  `:fr.jeremyschoffen.mbt.alpha.maven.credentials/password`

### Spec:
```clojure
string?
```
### Description:


Password used in maven credentials.



##  `:fr.jeremyschoffen.mbt.alpha.maven.credentials/private-key`

### Spec:
```clojure
path?
```
### Description:


Private key used in maven credentials.



##  `:fr.jeremyschoffen.mbt.alpha.maven.credentials/user-name`

### Spec:
```clojure
string?
```
### Description:


User name used in maven credentials.



##  `:fr.jeremyschoffen.mbt.alpha.maven.deploy/artefact`

### Spec:
```clojure
(keys
 :req
 [:fr.jeremyschoffen.mbt.alpha.maven.deploy.artefact/path
  :fr.jeremyschoffen.mbt.alpha.maven.deploy.artefact/extension])
```
### Description:


Maven artefact in the deploy sense. Basically a representation of a file
that will be installed / deployed by maven.


### Constructors:

- [[fr.jeremyschoffen.mbt.alpha.core/maven-sign-artefact!]]
- [[fr.jeremyschoffen.mbt.alpha.core.maven.common/sign-artefact!]]


##  `:fr.jeremyschoffen.mbt.alpha.maven.deploy/artefacts`

### Spec:
```clojure
(coll-of :fr.jeremyschoffen.mbt.alpha.maven.deploy/artefact)
```
### Description:


Collection of maven deploy artefacts to be installed / deployed.


### Constructors:

- [[fr.jeremyschoffen.mbt.alpha.core/maven-sign-artefacts!]]
- [[fr.jeremyschoffen.mbt.alpha.core.maven.common/sign-artefacts!]]
- [[fr.jeremyschoffen.mbt.alpha.default.maven/make-usual-artefacts]]
- [[fr.jeremyschoffen.mbt.alpha.default.maven/make-usual-artefacts+signatures!]]
- [[fr.jeremyschoffen.mbt.alpha.default.maven.common/make-usual-artefacts]]
- [[fr.jeremyschoffen.mbt.alpha.default.maven.common/make-usual-artefacts+signatures!]]


##  `:fr.jeremyschoffen.mbt.alpha.maven.deploy/sign-artefacts?`

### Spec:
```clojure
boolean?
```
### Description:


Option to sign artefacts when deploying them.



##  `:fr.jeremyschoffen.mbt.alpha.maven.deploy.artefact/extension`

### Spec:
```clojure
string?
```
### Description:


Extension of a deployment artefact.



##  `:fr.jeremyschoffen.mbt.alpha.maven.deploy.artefact/path`

### Spec:
```clojure
path?
```
### Description:


Path to a deployment artefact.



##  `:fr.jeremyschoffen.mbt.alpha.maven.install/dir`

### Spec:
```clojure
path?
```
### Description:


Directory where we want maven installation to put artefacts.


### Constructors:

- [[fr.jeremyschoffen.mbt.alpha.default.config.maven/maven-install-dir]]


##  `:fr.jeremyschoffen.mbt.alpha.maven.pom/path`

### Spec:
```clojure
path?
```
### Description:


Path to the `pom.xml` file to be used / synced.


### Constructors:

- [[fr.jeremyschoffen.mbt.alpha.default.config.maven/pom-path]]


##  `:fr.jeremyschoffen.mbt.alpha.maven.pom/properties`

### Spec:
```clojure
string?
```
### Description:


A `pom.properties` to be put in jars.


### Constructors:

- [[fr.jeremyschoffen.mbt.alpha.core/maven-new-pom-properties]]
- [[fr.jeremyschoffen.mbt.alpha.core.maven.pom/new-pom-properties]]


##  `:fr.jeremyschoffen.mbt.alpha.maven.pom/xml`

### Spec:
```clojure
map?
```
### Description:


Content of a `pom.xml` file in data form (data used by `clojure.tools.xml`).


### Constructors:

- [[fr.jeremyschoffen.mbt.alpha.core/maven-get-pom]]
- [[fr.jeremyschoffen.mbt.alpha.core/maven-sync-pom!]]
- [[fr.jeremyschoffen.mbt.alpha.core.maven.pom/get-pom]]
- [[fr.jeremyschoffen.mbt.alpha.core.maven.pom/new-pom]]
- [[fr.jeremyschoffen.mbt.alpha.core.maven.pom/sync-pom!]]


##  `:fr.jeremyschoffen.mbt.alpha.maven.scm/connection`

### Spec:
```clojure
string?
```
### Description:


Scm data for maven poms.



##  `:fr.jeremyschoffen.mbt.alpha.maven.scm/developer-connection`

### Spec:
```clojure
string?
```
### Description:


Scm data for maven poms.



##  `:fr.jeremyschoffen.mbt.alpha.maven.scm/tag`

### Spec:
```clojure
string?
```
### Description:


Scm data for maven poms.



##  `:fr.jeremyschoffen.mbt.alpha.maven.scm/url`

### Spec:
```clojure
string?
```
### Description:


Scm data for maven poms.



##  `:fr.jeremyschoffen.mbt.alpha.maven.server/id`

### Spec:
```clojure
string?
```
### Description:


An id of a maven server. Typically the id used in maven's `settings.xml` files
to identify a server.



##  `:fr.jeremyschoffen.mbt.alpha.maven.server/url`

### Spec:
```clojure
url?
```
### Description:


Url to a maven server.



##  `:fr.jeremyschoffen.mbt.alpha.maven.settings/file`

### Spec:
```clojure
path?
```
### Description:


Location of the `settings.xml` file to use.


### Constructors:

- [[fr.jeremyschoffen.mbt.alpha.default.config.maven/maven-settings-file]]


##  `:fr.jeremyschoffen.mbt.alpha.project/author`

### Spec:
```clojure
string?
```
### Description:


Author of the project. Used in jar manifests.


### Constructors:

- [[fr.jeremyschoffen.mbt.alpha.default.config.project/project-author]]


##  `:fr.jeremyschoffen.mbt.alpha.project/deps`

### Spec:
```clojure
(keys
 :opt-un
 [:clojure.tools.deps.alpha.specs/paths
  :clojure.tools.deps.alpha.specs/deps
  :clojure.tools.deps.alpha.specs/aliases])
```
### Description:


The deps map of the project.


### Constructors:

- [[fr.jeremyschoffen.mbt.alpha.core/deps-get]]
- [[fr.jeremyschoffen.mbt.alpha.core.deps/get-deps]]


##  `:fr.jeremyschoffen.mbt.alpha.project/license`

### Spec:
```clojure
(keys
 :req
 [:fr.jeremyschoffen.mbt.alpha.project.license/name
  :fr.jeremyschoffen.mbt.alpha.project.license/url
  :fr.jeremyschoffen.mbt.alpha.project.license/distribution]
 :opt
 [:fr.jeremyschoffen.mbt.alpha.project.license/comment
  :fr.jeremyschoffen.mbt.alpha.project.license/file])
```
### Description:


Data for a license entry in pom files.



##  `:fr.jeremyschoffen.mbt.alpha.project/licenses`

### Spec:
```clojure
(coll-of :fr.jeremyschoffen.mbt.alpha.project/license)
```
### Description:


Collection of the license entries for the pom file.



##  `:fr.jeremyschoffen.mbt.alpha.project/name`

### Spec:
```clojure
string?
```
### Description:


Name of the project. Used to generate git tags and maven artefact id.


### Constructors:

- [[fr.jeremyschoffen.mbt.alpha.default.config.project/project-name]]


##  `:fr.jeremyschoffen.mbt.alpha.project/output-dir`

### Spec:
```clojure
path?
```
### Description:


The directory in which the build tool outputs stuff.


### Constructors:

- [[fr.jeremyschoffen.mbt.alpha.default.config.project/output-dir]]


##  `:fr.jeremyschoffen.mbt.alpha.project/version`

### Spec:
```clojure
(and string? seq)
```
### Description:


Version of the project. Used in maven related activities.



##  `:fr.jeremyschoffen.mbt.alpha.project/working-dir`

### Spec:
```clojure
(every-pred path-like? absolute?)
```
### Description:


Working directory / root of the project.


### Constructors:

- [[fr.jeremyschoffen.mbt.alpha.default.config.project/working-dir]]


##  `:fr.jeremyschoffen.mbt.alpha.project.deps/aliases`

### Spec:
```clojure
(coll-of keyword? :into #{})
```
### Description:


Aliases to be used in conjunction with the project's deps.



##  `:fr.jeremyschoffen.mbt.alpha.project.deps/file`

### Spec:
```clojure
path?
```
### Description:


Location of the `deps.edn` file to use.


### Constructors:

- [[fr.jeremyschoffen.mbt.alpha.default.config.project/deps-file]]


##  `:fr.jeremyschoffen.mbt.alpha.project.license/comment`

### Spec:
```clojure
string?
```
### Description:


License data in poms.



##  `:fr.jeremyschoffen.mbt.alpha.project.license/distribution`

### Spec:
```clojure
license-distros
```
### Description:


License data in poms.



##  `:fr.jeremyschoffen.mbt.alpha.project.license/file`

### Spec:
```clojure
path?
```
### Description:


Path to the license file. (Will be included in jars.)



##  `:fr.jeremyschoffen.mbt.alpha.project.license/name`

### Spec:
```clojure
string?
```
### Description:


License data in poms.



##  `:fr.jeremyschoffen.mbt.alpha.project.license/url`

### Spec:
```clojure
string?
```
### Description:


License data in poms.



##  `:fr.jeremyschoffen.mbt.alpha.shell/command`

### Spec:
```clojure
(cat
 :cmd
 (* string?)
 :opts
 (* (cat :opt-name keyword? :opt-value any?)))
```
### Description:


Command to be run in another process. Basically:
```clojure
(apply clojure.java.shell/sh command)
```



##  `:fr.jeremyschoffen.mbt.alpha.shell/err`

### Spec:
```clojure
string?
```
### Description:


Key in the return value of a shell process.



##  `:fr.jeremyschoffen.mbt.alpha.shell/exit`

### Spec:
```clojure
int?
```
### Description:


Key in the return value of a shell process.



##  `:fr.jeremyschoffen.mbt.alpha.shell/out`

### Spec:
```clojure
string?
```
### Description:


Key in the return value of a shell process.



##  `:fr.jeremyschoffen.mbt.alpha.shell/result`

### Spec:
```clojure
(keys
 :req-un
 [:fr.jeremyschoffen.mbt.alpha.shell/exit
  :fr.jeremyschoffen.mbt.alpha.shell/out
  :fr.jeremyschoffen.mbt.alpha.shell/err])
```
### Description:


Return value of a shell process.



##  `:fr.jeremyschoffen.mbt.alpha.version-file/ns`

### Spec:
```clojure
symbol?
```
### Description:


Namespace of the version file.



##  `:fr.jeremyschoffen.mbt.alpha.version-file/path`

### Spec:
```clojure
path?
```
### Description:


Where to put the version file.



##  `:fr.jeremyschoffen.mbt.alpha.versioning/bump-level`

### Spec:
```clojure
keyword?
```
### Description:


Bump level to use when releasing a new version of the project. Depends on the versioning
scheme used.



##  `:fr.jeremyschoffen.mbt.alpha.versioning/major`

### Spec:
```clojure
keyword?
```
### Description:


Optional marker that will be used when creating git tag names and maven artefact names.



##  `:fr.jeremyschoffen.mbt.alpha.versioning/scheme`

### Spec:
```clojure
(satisfies? VersionScheme %)
```
### Description:


The versioning scheme to use.



##  `:fr.jeremyschoffen.mbt.alpha.versioning/tag-base-name`

### Spec:
```clojure
string?
```
### Description:


The base part of generated tag names, `mbt-alpha` in `mbt-alpha-vX`


### Constructors:

- [[fr.jeremyschoffen.mbt.alpha.default.config.versioning/tag-base-name]]


##  `:fr.jeremyschoffen.mbt.alpha.versioning/version`

### Spec:
```clojure
any?
```
### Description:


A a representation of a version generated and used by the version scheme.


### Constructors:

- [[fr.jeremyschoffen.mbt.alpha.default/versioning-initial-version]]
- [[fr.jeremyschoffen.mbt.alpha.default/versioning-next-version]]
- [[fr.jeremyschoffen.mbt.alpha.default.versioning/next-version]]
- [[fr.jeremyschoffen.mbt.alpha.default.versioning/schemes-bump]]
- [[fr.jeremyschoffen.mbt.alpha.default.versioning/schemes-current-version]]
- [[fr.jeremyschoffen.mbt.alpha.default.versioning/schemes-initial-version]]
- [[fr.jeremyschoffen.mbt.alpha.default.versioning.git-state/next-version]]
- [[fr.jeremyschoffen.mbt.alpha.default.versioning.schemes/bump]]
- [[fr.jeremyschoffen.mbt.alpha.default.versioning.schemes/current-version]]
- [[fr.jeremyschoffen.mbt.alpha.default.versioning.schemes/initial-version]]

