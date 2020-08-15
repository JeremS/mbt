(ns ^{:author "Jeremy Schoffen"
      :doc "
Protocols used in to create a flexible way to approach the creation of jar sources.

See [[fr.jeremyschoffen.mbt.alpha.core.jar.sources]] to have an example of how to use these
protocols.
      "}
  fr.jeremyschoffen.mbt.alpha.core.jar.protocols)


(defprotocol JarSource
  "Protocol used make types capable of generating instances of
  [[fr.jeremyschoffen.mbt.alpha.core.jar.protocols/JarEntries]]."
  :extend-via-metadata true

  (to-entries [this] [this exclude]
    "Function making instances of [[fr.jeremyschoffen.mbt.alpha.core.jar.protocols/JarEntries]].

    Args:
    - `this`: the value out of which we want to make a
    [[fr.jeremyschoffen.mbt.alpha.core.jar.protocols/JarEntries]].
    - `exclude`: An exclusion function that will be specific to the
      [[fr.jeremyschoffen.mbt.alpha.core.jar.protocols/JarEntries]] being made. If this function returns true,
      the jar entry will be excluded from the jar. It must take only one argument which will be a map with the following
      keys:
      - `:jar.entry/src`: absolute path to the entry
      - `:jar.entry/dest`: relative path (to the dest jar) indicating where to place the entry in the jar
      - `:jar/temp-output`: path to the temporary directory"))


(defprotocol JarEntries
  "Protocol to implement in order to confer types the capacity to add files to a jar."
  :extend-via-metadata true
  (add! [this conf]
    "Add files to a jar according to what the implementer wants to add. Note that when implementing this function the
    use of [[fr.jeremyschoffen.mbt.alpha.core.jar.temp/add-entries]] is the obvious way.

    Args:
    - `this`: the instance of [[fr.jeremyschoffen.mbt.alpha.core.jar.protocols/JarEntries]] which contains
      its own logic as to what it wants to put in a jar.
    - `conf`: the build configuration in the form of a map. (see [[fr.jeremyschoffen.mbt.alpha.core.specs]])"))


(comment
  (clojure.repl/doc JarSource)
  (clojure.repl/doc to-entries)

  (clojure.repl/doc JarEntries)
  (clojure.repl/doc add!))
