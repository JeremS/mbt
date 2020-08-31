


# Rationale

The release of [tools.deps](https://github.com/clojure/tools.deps.alpha) opened the way for a plethora of build tools to be released, challenging
the previous [Leiningen](https://leiningen.org/) / [Boot](https://boot-clj.com/) duopoly.

This project aims at providing a build tools for clojure programs that takes advantage of [tools.deps](https://github.com/clojure/tools.deps.alpha).
Here is some of the reasoning behind the idea that build tools other than [Leiningen](https://leiningen.org/) / [Boot](https://boot-clj.com/) may be necessary and why
not going for one of the available solutions.

## Yet Another build tool?
The features of [tools.deps](https://github.com/clojure/tools.deps.alpha) (like git dependencies) and the fact that it is an official (or core) clojure project is
a strong incentive to adoption. Still we need to build, package and deploy our projects. As it is envisioned in the
[rationale](https://github.com/clojure/tools.deps.alpha/blob/master/README.md), using [tools.deps](https://github.com/clojure/tools.deps.alpha) in conjunction with a
build tool is then the way to go.

[Tools.deps](https://github.com/clojure/tools.deps.alpha) is purposefully orthogonal to building, packaging and deploying concerns. Using it with
pre-existing tools then makes sense and the [tools.deps wiki](https://github.com/clojure/tools.deps.alpha/wiki/Tools) points to 2 such projects. However The
[integration with boot](https://github.com/seancorfield/boot-tools-deps) recommends using other tools better suited
to [tools.deps](https://github.com/clojure/tools.deps.alpha). The [integration with leiningen](https://github.com/RickMoynihan/lein-tools-deps) lists a series of
trade-offs using this method.

With these integrations in mind it feels like [tools.deps](https://github.com/clojure/tools.deps.alpha) leaves a void that needs filling with some sort
of `tools.build`.

### Yet Another `tools.build`?
Several takes on what a `tools.build` candidate can be are listed on the [tools.deps wiki](https://github.com/clojure/tools.deps.alpha/wiki/Tools). Surveying them
while keeping [Leiningen](https://leiningen.org/) and [Boot](https://boot-clj.com/) in mind, I made (or agreed with) some observations:

- [Leiningen](https://leiningen.org/)
    - The declarative-ness [Leiningen](https://leiningen.org/) provides is spot on for simple tasks.
    - It gets more complicated to use for custom tasks with the need to use/develop plugins.
- [Boot](https://boot-clj.com/)
    - Nice programmatic model where a context is passed to build tasks. It is reminiscent of the [Ring](https://github.com/ring-clojure/ring) model
    for dealing http requests, you thread a context into a chain of build tasks.
    - Can make the whole build process one clojure program.
    - Lends itself well for use at the repl.
    - Using a task is just a matter of requiring a namespace and using a function, no plugin system required.
    - The fileset model and the need for tasks to be middleware can be a bit complicated.
- [Metav](https://github.com/jgrodziski/metav)
    - Neat way of handling the versioning process in a sane and systematic way using git. (Other tools have
    this feature but it was the project that introduced me to it).
- [Badigeon](https://github.com/EwenG/badigeon)
    - Has pretty much everything you want.
    - The api doesn't lend itself to be used in a ring like manner.
    - Has some assumptions about the structure of the project I don't necessarily share.
- cli base tools
    - Several of the tools proposed on the [tools.deps wiki](https://github.com/clojure/tools.deps.alpha/wiki/Tools) are designed with
    [tools.cli](https://github.com/clojure/tools.cli) in mind. This approach is neat, unix like,
    you can chain your build steps in a shell script.
    This however encourages a model where each build action is performed by its own clojure program, each step starting
    its own jvm when it's his turn to work.
    - Re-using the internal apis of the tools that I looked at felt cumbersome. These internal apis
    don't lend themselves to be used in a [Boot](https://boot-clj.com/) like fashion.
- Some Rich Hickey talks
    - The clojure community has been challenged with the [spec-ulation talk](https://www.youtube.com/watch?v=oyLBGkS5ICk&list=PLZdCLR02grLrEwKaZv-5QbUzK0zGKOOcr&index=4&t=0s). The alpha namespaces for
    [clojure.spec](https://github.com/clojure/spec.alpha/) and [tools.deps](https://github.com/clojure/tools.deps.alpha) or the versioning scheme of
    [Meander](https://github.com/noprompt/meander) are interesting. We can use Several major versions of these in the same classpath without them clashing.
    - The idea that positional semantics for function parameters is potentially a bad in the talk
    [Effective Programs - 10 Years of Clojure](https://www.youtube.com/watch?v=2V1FtfBDsLU&t=37m07s) is
    interesting. The [Ring](https://github.com/ring-clojure/ring) context idea resonates here. Just one map as parameter to functions, the actuals
    parameters being values all named by keys.

Wrestling with these observations led me to try and build something of my own.


