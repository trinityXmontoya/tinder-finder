(defproject tinder-finder "0.1.0-SNAPSHOT"
  :description "See README"
  :url "http://example.com/FIXME"
  :license {:name "MIT"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [http-kit "2.1.18"]
                 [cheshire "5.6.3"]
                 [com.taoensso/carmine "2.14.0"]
                 [com.taoensso/timbre "4.3.1"]
                 [environ "1.1.0"]]
  :main ^:skip-aot tinder-finder.core
  :target-path "target/%s"
  :plugins [[lein-environ "1.1.0"]]
  :profiles {:uberjar {:aot :all}})
