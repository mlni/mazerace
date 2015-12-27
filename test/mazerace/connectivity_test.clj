(ns mazerace.connectivity-test
  (:use [clojure.test])
  (:require [mazerace.connectivity :as con]
            [clojure.core.async :refer [close! <! <!! >! chan go go-loop]]))

(deftest connection-handler
  (testing "launches a game with two connections"
    (let [conn-ch (chan)
          game-started (atom false)
          done-ch (chan)
          game-fn (fn [_ _]
                    (reset! game-started true)
                    (close! done-ch))]
      (con/connection-worker conn-ch game-fn)
      (go
        (>! conn-ch [(chan) (chan)])
        (>! conn-ch [(chan) (chan)])
        (close! conn-ch))
      (<!! done-ch)                                         ; wait for result
      (is (true? @game-started))))

  (testing "detects dropped connections during waiting"
    (let [conn-ch (chan)
          [recv-a send-a] [(chan) (chan)]
          done-ch (chan)
          game-fn (fn [& _] (close! done-ch))]
      (con/connection-worker conn-ch game-fn)
      (go
        (>! conn-ch [recv-a send-a])
        (close! recv-a)
        (>! conn-ch [(chan) (chan)])
        (>! conn-ch [(chan) (chan)]))
      (is (nil? (<!! done-ch)))
      (go (close! conn-ch)))))