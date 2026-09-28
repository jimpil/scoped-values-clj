# scoped-values-clj

## What

A tiny (~100 LOC) Clojure library for leveraging `java.lang.ScopedValue`. 
It introduces 4 macros, and 2 functions.

1. `defscoped`: declares a scoped Var (like declaring a dynamic Var)
2. `scoping`: like `clojure.core/binding`, but for scoped Vars
3. `with-scope`: like `clojure.core/with-bindings`, but for scoped Vars
4. `scoped-fn*`: like `clojure.core/bound-fn*` (uses `with-scope`)
5. `scoped-fn`: like `clojure.core/bound-fn` (uses `scoped-fn*`)
6. `current-scope`: like `clojure.core/get-thread-bindings`

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

### Current scope
You can capture the current scope with `current-scope`, and reuse/propagate it with `with-scope`, however, 
chances are you probably want to use `scoped-fn` instead.

### Nesting
Re-scoping (i.e. nested `scoping`) is fully supported, just like re-binding (via `binding`).

## Caveats/Limitations ()

Unfortunately, when declaring scoped-values via `defscoped`, what you get is actually a wrapper type 
(i.e. `DerefableScopedValue`), which implements `clojure.lang.IDeref`. You see, it's kind of impossible to have 
a raw `java.lang.ScopedValue`, or a proxy of it, because:

1. `clojure.lang.IDeref` is **not** a protocol, so it cannot be extended
2. `java.lang.ScopedValue` is a **final** class, so it cannot be inherited from

Therefore, until the Clojure team adds native deref support (see [CLJ-2926](https://clojure.atlassian.net/browse/CLJ-2926)), 
we have to deal with some kind of wrapper type, and that presents a challenge, because now, code that deals with instances 
created via `defscoped` VS _native_ ones, has to look different!More specifically, you cannot call `deref` on a _native_ 
`java.lang.ScopedValue`, so you have to manually call `.get` on it.

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
