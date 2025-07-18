(ns scoped-values-clj.core-test
  (:require [clojure.test :refer :all]
            [scoped-values-clj.core :refer :all]))

(defscoped $NAME$ "Scoped name doc-string")
(defscoped $LANG$ "Scoped language doc-string")

(defn- do-something! []
  (with-scoped-vars
    (println "Name is" $NAME$)
    (println "Lang is" $LANG$)
    ::done))

(defn- do-something-else! []
  (scoping [$NAME$ "lambda"
            $LANG$ "clojure"]
    (do-something!)))

(deftest scoping-tests
  (testing "`scoping` return value"
    (let [ret  (scoping [$NAME$ "duke"
                         $LANG$ "java"]
                 (do-something!))]
      (is (= ::done ret))))

  (testing "`scoping` bindings"
    (let [out-str  (with-out-str
                     (scoping [$NAME$ "duke"
                               $LANG$ "java"]
                       (do-something!)))
          expected (str "Name is duke" \newline
                        "Lang is java" \newline)]
      (is (= expected out-str))))

  (testing "nested scoping"
    (let [out-str  (with-out-str
                     (scoping [$NAME$ "duke"
                               $LANG$ "java"]
                       (do-something!)
                       (do-something-else!)))
          expected (str "Name is duke"    \newline
                        "Lang is java"    \newline
                        "Name is lambda"  \newline
                        "Lang is clojure" \newline)]
      (is (= expected out-str))))
  )
