(defproject cladura "0.1.0-SNAPSHOT"
  :description "A web application providing a general structure for rehearsing facts"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0-alpha1"]
                 [com.datomic/datomic-free "0.9.4609"]]
  :profiles {:dev {:dependencies [[midje "1.6.3"]]}})
