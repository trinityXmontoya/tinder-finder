(defproject tinder-finder "0.1.0-SNAPSHOT"
  :description "See README"
  :url "https://github.com/trinityXmontoya/tinder-finder"
  :license {:name "Creative Commons Attribution-NonCommercial-ShareAlike 4.0 International"
            :url "https://creativecommons.org/licenses/by-nc-sa/4.0/legalcode"
            :author "Trinity Montoya"}
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
