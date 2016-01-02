(ns mazerace.maze.kruskal-test
  (:use [clojure.test])
  (:require [mazerace.maze.kruskal :as k]
            [mazerace.maze.core :as c]
            [mazerace.solver :as solver]))

(deftest generating-mazes-with-kruskals
  (testing "Generating a trivial maze"
    (let [maze (k/generate 3 1)
          expected (c/compact [[{:u true :l true :d true}
                                {:u true :d true}
                                {:u true :r true :d true}]])]
      (is (= maze expected))))

  (testing "Generating a 5x5 maze"
    (let [maze (k/generate 5 5)
          solution (solver/find-shortest-path maze [0 0] [4 4])]
      (is (not (nil? solution))))))