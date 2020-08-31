


# Design
Here are some design ideas &amp; principles that guided the development or emerged while coding mbt. They are
informed by the observations made in the rationale.

## The base api's *shape*
Mbt's apis try to be as uniform as possible by providing functions of one argument. This argument is a
context / config map making every effective argument a named one.

This model allows for the following properties:
- There are no positional semantics in the api's functions. Every datum pertinent to the build process is named
- We can declare a config map similar to [Leiningen's](https://leiningen.org/) config as a build context
- We can have a simple (easy?) model for build tasks:
  - A build task can be a function of a config
  - We can thread a context through several successive build tasks
  - This model lends itself for use at the repl
  - We could use middleware for some tasks but we don't have to.
  - We can make a cli in front of build scripts but we don't have to.
  - If anything in this model doesn't work we can still do something else with the core functionality.
- As few assumptions as possible are made:
  - Beside some standard file names in jar everything should be parameterized
  - The cleaning utility will neither delete the project directory nor delete outside of it.


## Versioning
The process of building artefacts involves versioning. There is support for a pluggable versioning schemes
using git tags in the default api. Maven and Semver are there...

There is also very basic support designed with the [spec-ulation talk](https://www.youtube.com/watch?v=oyLBGkS5ICk&list=PLZdCLR02grLrEwKaZv-5QbUzK0zGKOOcr&index=4&t=0s) and the [Meander project](https://github.com/noprompt/meander) in mind.
The idea is to have named Major versions which are reflected in namespaces and in artefact names.
Also we must never introduce breaking changes inside a major version, Only provide more or require less.
(See the [spec-ulation talk](https://www.youtube.com/watch?v=oyLBGkS5ICk&list=PLZdCLR02grLrEwKaZv-5QbUzK0zGKOOcr&index=4&t=0s)).

For instance this project is named "mbt" and has a major version name `alpha`. The git tags
and maven artefacts generated will be `mbt-alpha(something)?`. In the alpha version
the code lives in `...mbt.alpha.xxx` namespaces. Configuration keywords and specs are also using a full
`:...mbt.alpha.XXX/YYY` style.

This way when we want to provide a new major version i.e., a new thing i.e, breakage, we provide a new Major version
named for instance `beta`. We would then have `mbt-beta` artefacts with code living under `...mbt.beta.xxx`
namespaces and `:...mbt.beta.XXX/YYY` keywords.
It allows to have an arbitrary number of major versions living peacefully in the same classpath
and so reduce dependency hell. At least that's the idea.


## Use of namespace qualified keywords
To impose that the keywords used in the program all be namespace qualified can be a bit unwieldy at first. Especially
considering:
- if we follow the java convention and the versioning convention we can end up with `::name` expanding to
something like `:com.domain.project.version.actual.ns/name`
- such long keywords have a a namespace doing to things:
  - the `com.domain.project.version` is there to avoid clashes
  - the `actual.ns` should be a domain specific context for the keyword's name
- the `actual.ns` part isn't necessarily  what we want. It comes from the namespaces where `::name` is used
- It makes for long keywords...

The solution used in mbt is to provide macros simplifying this kind of stuff:
```clojure

(create-ns 'com.domain.project.version.user)
(alias 'user 'com.domain.project.version.user)

::user/name
;=> :com.domain.project.version.user/name


(fr.jeremyschoffen.mbt.alpha.utils/pseudo-nss project)
::project/name
;=> fr.jeremyschoffen.mbt.alpha.project/name

```


## Stratified design
There is a lot here that seems common wisdom. Still it might be good to state the obvious and see how mbt relates.

### Low level core api
Mbt comes with a low level core api. The core utilities must be as un-opinionated and orthogonal as possible. This is
the no assumptions part of the api.

For instance the core api provides:
  - a function to compute a classpath from edn data (in the `deps.edn` format)
  - a function that turns a classpath into a sequence of sources to be put inside a jar.
  - a function taking jar sources and copying them into a temporary directory
  - a function that zips the temp dir into a jar.

None of these functions depend on one another. The one map argument principle allows a coding model where we can
thread together several utilities on a config / context map. Several helper functions are provided to make this process
easier.

### Default higher level api
Using just the core api can be tedious. The goal here is to provide a gluing of the core api in a sensible default
fashion. For instance there is a function that encompasses all the steps cited in the
previous example.

Still any build process that needs more complexity than what the default api provides can use the core api to define
its own build tool and still use the default api where useful.

## Credits
This project takes a lot from different projects whether in design ideas or directly in the way to code specific things:

- [Boot](https://boot-clj.com/) / [Ring](https://github.com/ring-clojure/ring) for the threaded context model
- [Cambada](https://github.com/luchiniatwork/cambada) for the compilation code.
- [Depstar](https://github.com/seancorfield/depstar) for the idea that we can build a jar from a classpath.
- [Depstar](https://github.com/seancorfield/depstar) / [Cambada](https://github.com/luchiniatwork/cambada) / [Badigeon](https://github.com/EwenG/badigeon) for the jar building code.
- [Boot](https://boot-clj.com/) / [Badigeon](https://github.com/EwenG/badigeon) for the gpg code
- [Badigeon](https://github.com/EwenG/badigeon) for the manifest code
- [Metav](https://github.com/jgrodziski/metav) for the git versioning code.
- [Meander](https://github.com/noprompt/meander) / [tools.deps](https://github.com/clojure/tools.deps.alpha) as a source of inspiration for the versioning scheme
