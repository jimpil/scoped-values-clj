(defproject scoped-values-clj "0.1.0-SNAPSHOT"
  :description "Facilities for leveraging java.lang.ScopedValue in Clojure"
  :url "https://github.com/jimpil/scoped-values-clj"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.12.3"]
                 [org.clojure/tools.macro "0.2.0"]]
  :repl-options {:init-ns scoped-values-clj.core})
