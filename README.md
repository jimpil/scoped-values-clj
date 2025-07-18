# scoped-values-clj

## What

A Clojure library for dealing with `java.lang.ScopedValue`. 
It introduces 3 macros (40 LOC), and a naming convention (i.e. `$...$`).

1. `defscoped`: declares a scoped-var
2. `scoping`: (re)binds one or more scoped-vars
3. `with-scoped-vars`: wraps symbols referring to scoped-vars with `(.get ...)`

## Why
Without getting into too much detail, `ScopedValue` is essentially a better `ThreadLocal`,
especially in-lieu of virtual-threads. See the [JEP](https://openjdk.org/jeps/446).

## How
Similarly to how dynamic-vars must start/end with `*`, 
scoped-vars must start/end with `$`. So let's declare a couple:

```clj
(defscoped $NAME$ "Scoped name doc-string")
(defscoped $LANG$ "Scoped language doc-string")
```
With these in place, we can now scope them:

```clj
(scoping [$NAME$ "duke"
          $LANG$ "java"]
  (do-something!))
```
where `do-something!` can be anything that (presumably) reads the scoped-vars:

```clj
(defn do-something! []
  (with-scoped-vars
    (println "Name is" $NAME$)
    (println "Lang is" $LANG$)
    :done))
```
The important thing here is the use of `with-scoped-vars` which will turn 
`$NAME$` & `$LANG$` into `(.get $NAME$)` & `(.get $LANG$)` respectively.

### Nesting
Re-scoping (i.e. nested `scoping`) is supported, as long as a `scoping` expression
doesn't appear within the literal body of a `with-scoped-vars` expression (see next section for details).

## Limitations/Caveats

In the example above, there is a single level of scoping. But what if `do-something!` needed,
for some reason, to do its own scoping? Well, easy but pay attention to the positioning - 
it **can't** go under `with-scoped-vars`: 

```clj
(defn do-something! []
  (scoping [$NAME$ "foo"
            $LANG$ "clojure"]
    (with-scoped-vars
      (println "Name is" $NAME$)
      (println "Lang is" $LANG$)
      :done)))
```
In case it's not clear, this caveat is due to the nature of `with-scoped-vars` which will
replace symbols looking like scoped-vars, with their `(.get ...)` expr. Therefore, the literal body
of `with-scoped-vars` can only contain code that _reads_ scoped-vars.

### tl;dr
Avoid putting `scoping` expressions directly inside the body of `with-scoped-vars`. 
If they must appear within the same function, make sure that `scoping` is on top (like the example above), 
otherwise pull the relevant code into a separate function, and re-scope there:

```clj
(defn do-something! []
  (with-scoped-vars
    (do-something-else!) ;; <== re-scope in here
    (println "Name is" $NAME$)
    (println "Lang is" $LANG$)
    :done))
```
You can, of course, simply not use `with-scoped-vars`, and this problem goes away. 
However, in that case you have to remember to call `.get()` on scoped-vars (at the read-site).

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
