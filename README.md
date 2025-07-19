# scoped-values-clj

## What

A tiny (30 LOC) Clojure library for leveraging `java.lang.ScopedValue`. 
It introduces 2 macros, and a naming convention (i.e. `$...$`).

1. `defscoped`: declares a scoped-var
2. `scoping`: (re)binds one or more (already declared) scoped-vars

## Why
Without getting into too much detail, `ScopedValue` is essentially a better `ThreadLocal`,
especially in-lieu of virtual-threads. See the [JEP](https://openjdk.org/jeps/446).

## How
I like to name such 'global' vars with upper-case, to make them stand out visually 
(similarly to how dynamic vars must start/end with `*`). So let's declare a couple:

```clj
(defscoped NAME "Some doc-string")
(defscoped LANG "Some other doc-string")
```
With these in place, we can now scope them:

```clj
(scoping [NAME "duke"
          LANG "java"]
  (do-something!))
```
where `do-something!` can be anything that (presumably) reads the scoped-var(s):

```clj
(defn do-something! []
  (println "Name is" @NAME)
  (println "Lang is" @LANG)
  :done)
```
The important thing here is the use of `@` (i.e. `deref`), which is the equivalent of `ScopedValue::get` in Java.

### Nesting
Re-scoping (i.e. nested `scoping`) is fully supported, just like re-binding (via `binding`).

## Requirements

- Java 24 (or greater)
- Clojure 1.12.0 (or greater)

## License

Copyright © 2025 Dimitrios Piliouras

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.

This Source Code may also be made available under the following Secondary
Licenses when the conditions for such availability set forth in the Eclipse
Public License, v. 2.0 are satisfied: GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or (at your
option) any later version, with the GNU Classpath Exception which is available
at https://www.gnu.org/software/classpath/license.html.
