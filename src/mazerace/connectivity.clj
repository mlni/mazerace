(ns mazerace.connectivity
  (:require [clojure.core.async :refer [>! <! >!! <!! go chan close! thread alts! alts!! timeout go-loop]]
            [clojure.tools.logging :as log]
            [mazerace.game :as game]
            [mazerace.solver :as solver]))

(def incoming-connections (chan))

(defn handle-connection [recv-ch send-ch]
  (go (>! incoming-connections [recv-ch send-ch])))

(defn connection-worker [connections-ch game-fn]
  (go-loop [conns []]
    (if (= 2 (count conns))
      (let [[a b] conns]
        (log/info "Got two players, launching game")
        (game-fn a b)
        (recur []))
      (let [timeout-ch (timeout 15000)
            [v ch] (alts! (concat [connections-ch] (map first conns) (if (> (count conns) 0) [timeout-ch] [])))]
        (if (= ch connections-ch)
          (if (nil? v)
            (log/info "Shutting down")
            (recur (conj conns v)))
          (if (= ch timeout-ch)
            (do
              (log/debug "timeout waiting for opponent, starting computer player")
              (recur (conj conns (solver/computer-player))))
            (recur (if (nil? v)
                     (filter (fn [[recv-ch _]] (not= recv-ch ch)) conns)
                     conns))))))))

(defn start-connection-handler []
  (connection-worker incoming-connections game/start-game))
