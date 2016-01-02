(ns mazerace.connectivity
  (:require [clojure.core.async :refer [>! <! >!! <!! go chan close! thread alts! alts!! timeout go-loop]]
            [clojure.tools.logging :as log]
            [mazerace.game :as game]
            [mazerace.solver :as solver]))

(def incoming-connections (chan))

(defn handle-connection [recv-ch send-ch]
  (go (>! incoming-connections [recv-ch send-ch])))

(defn- dispatch-game [game-fn connections]
  (let [[a b] (take 2 connections)]
    (log/info "Got two players, launching game")
    (game-fn a b)
    (drop 2 connections)))

(defn connection-worker [connections-ch game-fn]
  (go
    (loop [conns []]
      (if (= 2 (count conns))
        (recur (dispatch-game game-fn conns))
        (let [timeout-ch (timeout 15000)
              [v ch] (alts! (concat [connections-ch] (map first conns) (if (> (count conns) 0) [timeout-ch] [])))]
          (condp = ch
            timeout-ch (do
                         (log/debug "timeout waiting for opponent, starting computer player")
                         (recur (conj conns (solver/computer-player))))
            connections-ch (when-not (nil? v)
                             (recur (conj conns v)))
            (recur (if (nil? v)
                     (filter (fn [[recv-ch _]] (not= recv-ch ch)) conns)
                     (if (= (:opponent v) "computer")
                       (conj conns (solver/computer-player))
                       conns)))))))
    (log/info "Shutting down")))

(defn start-connection-handler []
  (connection-worker incoming-connections game/start-game))
