(defproject mazerace "0.1.0"
  :description "Maze race the game"
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/clojurescript "1.7.170"]
                 [org.clojure/core.async "0.2.374"]
                 [org.clojure/data.json "0.2.6"]
                 [ring/ring-core "1.1.8"]
                 [ring/ring-devel "1.1.8"]
                 [compojure "1.0.2"]
                 [http-kit "2.1.19"]
                 [reagent "0.5.1" :exclusions [cljsjs/react]]
                 [cljsjs/react-with-addons "0.13.3-0"]
                 [secretary "1.2.3"]
                 [org.clojure/tools.logging "0.2.6"]
                 [log4j/log4j "1.2.17"]]
  :min-lein-version "2.0.0"
  :source-paths ["src" "src-ui"]
  :main mazerace.core
  :aot [mazerace.core]
  :plugins [[lein-cljsbuild "1.1.1"]]
  :clean-targets ^{:protect false} [:target-path "resources/public/main.js"]
  :hooks [leiningen.cljsbuild]
  :uberjar-name "mazerace.jar"
  :profiles {:uberjar {:cljsbuild {:builds [{:source-paths ["src-ui"]
                                             :compiler     {:output-to     "resources/public/main.js"
                                                            :optimizations :advanced
                                                            :pretty-print  false}}]}}
             :dev     {:cljsbuild {:builds [{:source-paths ["src-ui"]
                                             :compiler     {:output-to     "resources/public/main.js"
                                                            :optimizations :whitespace
                                                            :pretty-print  true}}]}}}
  )
