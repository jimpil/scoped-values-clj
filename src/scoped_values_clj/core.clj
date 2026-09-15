(ns scoped-values-clj.core
  (:require [clojure.tools.macro :as tools.macro])
  (:import [clojure.lang IDeref]
           [java.lang ScopedValue ScopedValue$Carrier]))

(set! *warn-on-reflection* true)

(deftype DerefableScopedValue
  [^ScopedValue v]
  IDeref
  (deref [_] (when (.isBound v) (.get v))))

(defn ->DerefableScopedValue
  ^DerefableScopedValue []
  (-> (ScopedValue/newInstance)
      (DerefableScopedValue.)))

(defn unwrap*
  ^ScopedValue [^DerefableScopedValue dsv]
  (.v dsv))

(defmacro defscoped
  [sym & args]
  (let [[symb body] (tools.macro/name-with-attributes
                      (vary-meta sym assoc :tag `DerefableScopedValue)
                      (concat args [`(->DerefableScopedValue)]))]
    `(def ~symb ~@body)))

(defn- carrier*
  [bindings]
  (let [[[s v] & more] (partition 2 bindings)]
    (reduce
      (fn [c [s v]]
        `(.where ~c (unwrap* ~s) ~v))
      `(ScopedValue/where (unwrap* ~s) ~v)
      more)))

;; Implement the notion current-scope using a ScopedValue
(defonce SCOPED-VARS (->DerefableScopedValue))
(defonce deref2 (comp deref deref))

(defn current-scope
  "Captures the current scope (if any).
   Returns a map of Var => bound-value."
  []
  (let [vars @SCOPED-VARS]
    (zipmap vars (map deref2 vars))))

(defn- call*
  [^ScopedValue$Carrier carrier thunk]
  (let [nil-sentinel (Object.)
        ret (.call carrier
              (fn [] ;;can't return nil from in here
                (if-some [body-ret (thunk)]
                  body-ret
                  nil-sentinel)))]
    (if (identical? ret nil-sentinel)
      nil
      ret)))

(defmacro scoping
  "Like `clojure.core/binding, but for `ScopedValue`, rather than `ThreadLocal`."
  [bindings & body]
  (assert (vector? bindings) "`scoping` expects a vector of <bindings>")
  (assert (even? (count bindings)) "`scoping` expects an even number of <bindings>")
  `(call*
     ~(carrier*
        (conj bindings
              `SCOPED-VARS
              `(into (or @SCOPED-VARS #{})
                     ~(mapv resolve (take-nth 2 bindings)))))
     (fn [] ~@body)))

(defmacro with-scope
  "Executes <body> within the context of the
   provided <scope> - e.g. captured by `current-scope`.
   Useful for passing down scope to child threads.
   The pattern is as follows:
   (scoping [...]                  ;; set up initial bindings
     (let [parent (current-scope)] ;; fetch current scope in this thread
       (future
         (with-scope parent        ;; restore parent's scope in child thread
           (current-scope)))))     ;; inspect current-scope in child thread (equals parent)"
  [scope & body]
  `(if-some [curr# (not-empty ~scope)]
     (let [[[s# v#] & more#] curr#
           carrier# (reduce
                      (fn [~(with-meta 'c {:tag `ScopedValue$Carrier}) [s# v#]]
                        (.where ~'c (unwrap* @s#) v#))
                      (ScopedValue/where (unwrap* @s#) v#)
                      (concat more# [[#'SCOPED-VARS (set (keys curr#))]]))]
          (call* carrier# (fn [] ~@body)))
     (do ~@body)))

(defn scoped-fn*
  "Like `clojure.core/bound-fn*`, but for ScopedValue (instead of ThreadLocal)."
  [f]
  (let [current (current-scope)]
    (fn [& args]
      (with-scope current (apply f args)))))

(defmacro scoped-fn
  "Like `clojure.core/bound-fn`, but for ScopedValue (instead of ThreadLocal)."
  [& fntail]
  `(scoped-fn* (fn ~@fntail)))
