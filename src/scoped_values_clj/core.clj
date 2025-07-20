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

(defn carrier*
  [bindings]
  (let [[[s v] & more] (partition 2 bindings)]
    (reduce
      (fn [c [s v]]
        `(.where ~c (.v ~s) ~v))
      `(ScopedValue/where (.v ~s) ~v)
      more)))

(defmacro scoping
  "Like `clojure.core/binding, but for `ScopedValue`, rather than `ThreadLocal`."
  [bindings & body]
  (assert (vector? bindings) "`scoping` expects a vector of <bindings>")
  (assert (even? (count bindings)) "`scoping` expects an even number of <bindings>")
  `(let [nil# (Object.)
         ret# (-> ~(carrier* bindings)
                   (.call (fn [] ;;can't return nil from in here
                            (if-some [x# (do ~@body)]
                              x#
                              nil#))))]
     (if (identical? ret# nil#)
       nil
       ret#)))
