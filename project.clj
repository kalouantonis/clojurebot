(defproject clojurebot "0.1.2"
  :description "An IRC bot with a bunch of (arguably) useful commands."
  :url "https://github.com/kalouantonis/clojurebot"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [clojail "1.0.6"]]
  :main ^:skip-aot clojurebot.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
