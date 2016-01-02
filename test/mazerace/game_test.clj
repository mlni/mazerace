(ns mazerace.game-test
  (:require [mazerace.game :as g]
            [mazerace.maze.core :as m])
  (:use [clojure.test]))

; [ _ _ _ _ _ ]
(def trivial-maze (m/compact [[{:u true :l true :d true}
                               {:u true :d true}
                               {:u true :d true}
                               {:u true :d true}
                               {:u true :r true :d true}]]))


(deftest find-differences
  (testing "with no differences"
    (let [a {:a 1}
          b {:a 1}
          diff (#'g/find-differences a b)]
      (is (nil? diff))))

  (testing "additions"
    (let [a {:a 1}
          b {:a 1 :b 2}
          diff (#'g/find-differences a b)]
      (is (= diff {:b 2}))))

  (testing "removals"
    (let [a {:a 1 :b 2}
          b {:a 1}
          diff (#'g/find-differences a b)]
      (is (= diff {:b nil}))))

  (testing "modifications"
    (let [a {:a [1 2 3]}
          b {:a [1 2 3 4]}
          diff (#'g/find-differences a b)]
      (is (= diff {:a [1 2 3 4]})))))


(deftest finish
  (let [game-wo-player {:maze   trivial-maze
                        :target [4 0]}]

    (testing "only acts when player has reached finish"
      (let [game (assoc game-wo-player :p1 {:position [0 0]})
            after (#'g/handle-finish game :p1)]
        (is (nil? (get-in after [:p1 :result])))))

    (testing "detects when player has reached the finish"
      (let [game (assoc game-wo-player :p1 {:position [4 0]})
            after (#'g/handle-finish game :p1)]
        (is (= :win (get-in after [:p1 :result])))))))


(deftest jumpers
  (testing "stepping on a jumper"
    (let [game {:maze    trivial-maze
                :jumpers [2 0]
                :p1      {:position [2 0]}}
          result (#'g/handle-jumpers game :p1)]
      (is (empty? (:jumpers result)))
      (is (not= (get-in game [:p1 :position])
                (get-in result [:p1 :position]))))))

(deftest throwers
  (testing "stepping on a thrower"
    (let [game {:maze     trivial-maze
                :throwers [1 0]
                :p1       {:position [1 0]}
                :p2       {:position [3 0]}}
          result (#'g/handle-throwers game :p1)]
      (is (empty? (:throwers result)))
      (is (not= (get-in game [:p2 :position])
                (get-in result [:p2 :position]))))))


(deftest collisions
  (testing "players colliding"
    (let [game {:maze trivial-maze
                :p1   {:position [1 0]}
                :p2   {:position [1 0]}}
          result (#'g/handle-player-collision game :p1)]
      (is (not= (get-in game [:p1 :position])
                (get-in result [:p1 :position])))
      (is (not= (get-in game [:p2 :position])
                (get-in result [:p2 :position]))))))

(deftest player-moves
  (let [game {:maze   trivial-maze
              :p1     {:position [0 0]}
              :p2     {:position [4 0]}
              :target [3 0]}]

    (testing "player moving a step"
      (let [[result p1-update p2-update] (#'g/player-move game :p1 [1 0])]
        (is (nil? p1-update))
        (is (contains? p2-update :opponent-position))
        (is (= [1 0] (get-in result [:p1 :position])))))

    (testing "player jumping multiple steps"
      (let [[result p1-update p2-update] (#'g/player-move game :p1 [2 0])]
        (is (nil? p1-update))
        (is (nil? p2-update))
        (is (= [0 0] (get-in result [:p1 :position])))))

    (testing "player finishing game"
      (let [result (reduce (fn [game move]
                             (first (#'g/player-move game :p1 move)))
                           game
                           [[1 0] [2 0] [3 0]])]
        (is (= :win (get-in result [:p1 :result])))
        (is (= :lose (get-in result [:p2 :result])))))))
