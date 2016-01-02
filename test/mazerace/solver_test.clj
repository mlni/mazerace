(ns mazerace.solver-test
  (:require [mazerace.solver :as solver]
            [mazerace.maze.core :as m]
            [mazerace.maze.kruskal :as maze])
  (:use [clojure.test]))

(deftest solver
  (testing "Finding paths"
    (testing "in trivial maze"
      ; [x _ y]
      (let [trivial-maze (m/compact [[{:u true :l true :d true}
                                      {:u true :d true}
                                      {:u true :r true :d true}]])
            solution (solver/find-shortest-path trivial-maze [0 0] [2 0])]
        (is (not (nil? solution)))
        (is (= 3 (count solution)))))

    (testing "in a maze with a turn"
      ; | x | y |
      ; |_______|
      (let [maze (m/compact [[{:u true :l true :r true} {:u true :r true}]
                             [{:l true :d true} {:d true :r true}]])
            solution (solver/find-shortest-path maze [0 0] [1 0])]
        (is (not (nil? solution)))
        (is (= 4 (count solution)))))

    (testing "in a generated 5x5 maze"
      (let [maze (maze/generate 5 5)
            solution (solver/find-shortest-path maze [0 0] [4 0])]
        (is (not (nil? solution)))))

    (testing "in a generated 10x10 maze"
      (let [maze (maze/generate 10 10)
            solution (solver/find-shortest-path maze [0 0] [9 9])]
        (is (not (nil? solution)))))

    (testing "in a generated 10x10 maze from a random point to another"
      (let [maze (maze/generate 10 10)
            solution (solver/find-shortest-path maze
                                                [(rand-int 10) (rand-int 10)]
                                                [(rand-int 10) (rand-int 10)])]
        (is (not (nil? solution)))))))