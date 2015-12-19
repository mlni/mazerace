(ns mazerace.connectivity
  (:require [clojure.core.async :as a :refer [>! <! >!! <!! go chan close! thread alts! alts!! timeout go-loop]]
            [clojure.tools.logging :as log]
            [mazerace.game :as game]
            [clojure.data.json :as json]))

(def incoming-connections (chan))

(defn handle-connection [recv-ch send-ch]
  (go (>! incoming-connections [recv-ch send-ch])))

(go-loop [conns []]
  (if (= 2 (count conns))
    (let [[a b] conns]
      (game/handle-game a b)
      (recur []))
    (let [[v ch] (alts! (concat [incoming-connections] (map first conns)))]
      (if (= ch incoming-connections)
        (recur (conj conns v))
        (recur (if (nil? v)
                 (filter (fn [[recv-ch _]] (not= recv-ch ch)) conns)
                 conns))))))
