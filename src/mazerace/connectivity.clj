(ns mazerace.connectivity
  (:require [clojure.core.async :as a :refer [>! <! >!! <!! go chan close! thread alts! alts!! timeout go-loop]]
            [clojure.tools.logging :as log]
            [mazerace.maze :as maze]
            [clojure.data.json :as json]))

(def incoming-connections (chan))

(defn handle-connection [recv-ch send-ch]
  (go (>! incoming-connections [recv-ch send-ch])))

(defn the-other [player]
  (if (= :p1 player) :p2 :p1))

(defn- place-randomly [game player]                         ; avoid the other player and the exit
  (let [other-pos (get-in game [(the-other player) :position])
        width (count (get-in game [:maze 0]))
        heigth (count (get-in game [:maze]))]
    (loop []
      (let [rand-x (rand-int width)
            rand-y (rand-int heigth)]
        (if (not= [rand-x rand-y] other-pos)
          [rand-x rand-y]
          (recur))))))

(defn handle-pair [[recv-a send-a] [recv-b send-b]]
  (log/info "Got pair, launching handler")
  (go
    (let [width 20
          height 20
          target-position [(quot width 2) (dec height)]
          maze (maze/generate width height)
          game (atom {:maze   maze
                      :p1     {:position [0 0]}
                      :p2     {:position [(dec width) 0]}
                      :target target-position})]
      (>! send-a {:maze              maze
                  :position          (get-in @game [:p1 :position])
                  :opponent-position (get-in @game [:p2 :position])
                  :target            target-position})
      (>! send-b {:maze              maze
                  :position          (get-in @game [:p2 :position])
                  :opponent-position (get-in @game [:p1 :position])
                  :target            target-position})
      (loop []
        (let [[msg chan] (alts! [recv-a recv-b])
              player (if (= chan recv-a) :p1 :p2)
              other-player (the-other player)]
          (if msg
            (do
              (log/info "Received " msg "from" player)
              (when (:move msg)
                (let [player-pos (:move msg)
                      other-pos (get-in @game [other-player :position])]
                  (log/debug player player-pos other-player other-pos)
                  (if (= player-pos other-pos)
                    (do
                      (log/info "Collision, randomizing positions")
                      (swap! game assoc-in [player :position] (place-randomly @game player))
                      (swap! game assoc-in [other-player :position] (place-randomly @game other-player))
                      (>! (if (= chan recv-a) send-a send-b) {:position          (get-in @game [player :position])
                                                              :opponent-position (get-in @game [other-player :position])})
                      (>! (if (= chan recv-a) send-b send-a) {:position          (get-in @game [other-player :position])
                                                              :opponent-position (get-in @game [player :position])}))
                    (if (= player-pos target-position)
                      (do
                        (log/info player "wins")
                        (>! (if (= player :p1) send-a send-b) {:result :win})
                        (>! (if (= player :p1) send-b send-a) {:result :lose}))
                      (do
                        (swap! game assoc-in [player :position] (:move msg))
                        (>! (if (= chan recv-a) send-b send-a) {:opponent-position (:move msg)}))))))
              ; (log/debug "after" player (get-in @game [player :position]) other-player (get-in @game [other-player :position]))
              (recur))
            ((log/info "cleaning up pair")
              (close! send-a)
              (close! send-b))))))))

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
