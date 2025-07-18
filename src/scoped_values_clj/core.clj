(ns scoped-values-clj.core
  (:require [clojure.string :as str]
            [clojure.walk :as walk]
            [clojure.tools.macro :as tools.macro])
  (:import [java.lang ScopedValue]))

(set! *warn-on-reflection* true)

(defn scoped-var?
  [sym]
  (let [sym-name (name sym)]
    (and (str/starts-with? sym-name "$")
         (str/ends-with? sym-name "$"))))

(defmacro defscoped
  [sym & args]
  (assert (scoped-var? sym) "Scoped vars must start & end with `$`")
  (let [[symb body] (tools.macro/name-with-attributes
                      (vary-meta sym assoc :tag 'java.lang.ScopedValue)
                      (concat args [`(ScopedValue/newInstance)]))]
    `(def ~symb ~@body)))

(defmacro with-scoped-vars
  "Replaces all scoped-var symbols with the expression `(.get ...)`.
   Necessary for seamlessly extracting the currently-bound value out of a `ScopedValue`."
  [& body]
  (cons
    'do
    (walk/postwalk
      (fn [x]
        (if (and (symbol? x)
                 (scoped-var? x))
          `(.get ~x)
          x))
      body)))

(defmacro scoping
  "Like `clojure.core/binding, but for `ScopedValue`, rather than `ThreadLocal`."
  [bindings & body]
  (assert (vector? bindings) "`scoping` expects a vector of <bindings>")
  (assert (even? (count bindings)) "`scoping` expects an even number of <bindings>")
  (let [[[s v] & more] (partition 2 bindings)
        carrier (reduce
                  (fn [c [s v]]
                    `(.where ~c ~s ~v))
                  `(ScopedValue/where ~s ~v)
                  more)]
    `(try ;; body may return nil, which will cause .call to throw NPE
       (.call ~carrier (fn [] ~@body))
       (catch NullPointerException _# nil))))

(defmacro scoping-locally
  "Like `scoping`, but expects all binding symbols to appear in <body>."
  [bindings & body]
  `(scoping ~bindings (with-scoped-vars ~@body)))
