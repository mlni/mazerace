(ns mazerace.maze.backtracker-test
  (:use [clojure.test])
  (:require [mazerace.maze.backtracker :as b]
            [mazerace.maze.core :as c]
            [mazerace.solver :as solver]))

(deftest maze-generation
  (testing "Generating trivial maze"
    ; [ _ _ _ ]
    (let [maze (b/generate 3 1 [2 0])
              expected (c/compact [[{:u true :l true :d true}
                                    {:u true :d true}
                                    {:u true :r true :d true}]])]
          (is (= maze expected))))

  (testing "Generating a 5x5 solvable maze"
    (let [maze (b/generate 5 5 [4 4])
          solution (solver/find-shortest-path maze [0 0] [4 4])]
      (is (not (nil? solution))))))