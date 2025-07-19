(ns scoped-values-clj.core
  (:require [clojure.tools.macro :as tools.macro])
  (:import [clojure.lang IDeref]
           [java.lang ScopedValue]))

(set! *warn-on-reflection* true)

(deftype DerefableScopedValue
  [^ScopedValue v]
  IDeref
  (deref [_] (.get v)))

(defn ->DerefableScopedValue
  ^DerefableScopedValue []
  (-> (ScopedValue/newInstance)
      (DerefableScopedValue.)))

(defmacro defscoped
  [sym & args]
  (let [[symb body] (tools.macro/name-with-attributes
                      (vary-meta sym assoc :tag `DerefableScopedValue)
                      (concat args [`(->DerefableScopedValue)]))]
    `(def ~symb ~@body)))

(defmacro scoping
  "Like `clojure.core/binding, but for `ScopedValue`, rather than `ThreadLocal`."
  [bindings & body]
  (assert (vector? bindings) "`scoping` expects a vector of <bindings>")
  (assert (even? (count bindings)) "`scoping` expects an even number of <bindings>")
  (let [[[s v] & more] (partition 2 bindings)
        carrier (reduce
                  (fn [c [s v]]
                    `(.where ~c (.-v ~s) ~v))
                  `(ScopedValue/where (.-v ~s) ~v)
                  more)]
    `(try ;; body may return nil, which will cause .call to throw NPE
       (.call ~carrier (fn [] ~@body))
       (catch NullPointerException _# nil))))
