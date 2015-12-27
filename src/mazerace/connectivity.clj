(ns mazerace.connectivity
  (:require [clojure.core.async :as a :refer [>! <! >!! <!! go chan close! thread alts! alts!! timeout go-loop]]
            [clojure.tools.logging :as log]
            [mazerace.game :as game]
            [mazerace.solver :as solver]
            [clojure.data.json :as json]))

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
        (let [[v ch] (alts! (concat [connections-ch] (map first conns)))]
          (if (= ch connections-ch)
            (if (nil? v)
              (log/info "Shutting down")
              (recur (conj conns v)))
            (recur (if (nil? v)
                     (filter (fn [[recv-ch _]] (not= recv-ch ch)) conns)
                     conns)))))))

(defn start-connection-handler []
  (connection-worker incoming-connections game/start-game))
