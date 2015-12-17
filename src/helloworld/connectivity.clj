(ns helloworld.connectivity
  (:require [clojure.core.async :as a :refer [>! <! >!! <!! go chan close! thread alts! alts!! timeout go-loop]]
            [clojure.tools.logging :as log]))

(def incoming-connections (chan))

(defn handle-connection [recv-ch send-ch]
  (go (>! incoming-connections [recv-ch send-ch])))

(defn handle-pair [[recv-a send-a] [recv-b send-b]]
  (log/info "Got pair, launching handler")
  (go
    (>! send-a "starting")
    (>! send-b "starting")
    (loop []
      (let [[msg chan] (alts! [recv-a recv-b])]
        (if msg
          (do
            (log/info "Received " msg "from" (= chan recv-a) ", sending")
            (>! (if (= chan recv-a) send-b send-a) msg)
            (recur))
          ((log/info "cleaning up pair")
            (close! send-a)
            (close! send-b)))))))

(go-loop [conns []]
  (if (= 2 (count conns))
    (let [[a b] conns]
      (handle-pair a b)
      (recur []))
    (let [[v ch] (alts! (concat [incoming-connections] (map first conns)))]
      (if (= ch incoming-connections)
        (recur (conj conns v))
        (recur (if (nil? v)
                 (filter (fn [[recv-ch _]] (not= recv-ch ch)) conns)
                 conns))))))
