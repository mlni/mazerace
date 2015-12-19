(ns mazerace.game
  (:require [clojure.tools.logging :as log]
            [clojure.core.async :as a :refer [>! <! >!! <!! go chan close! thread alts! alts!! timeout go-loop]]
            [mazerace.maze :as maze]
            [mazerace.maze :as maze]))

(defn- the-other [player]
  (if (= :p1 player) :p2 :p1))

(defn- place-randomly [[width height] avoid-positions]
  (loop []
    (let [rand-x (rand-int width)
          rand-y (rand-int height)]
      (if (some #(= [rand-x rand-y] %) avoid-positions)
        (recur)
        [rand-x rand-y]))))

(defn- place-player-randomly [game player]                  ; avoid the other player and the exit
  (let [other-pos (get-in game [(the-other player) :position])
        target (get-in game [:target])
        avoid (concat (get-in game [:jumpers])
                      (get-in game [:throwers])
                      [other-pos target])
        width (count (get-in game [:maze 0]))
        height (count (get-in game [:maze]))]
    (place-randomly [width height] avoid)))


; TODO: split this up into:
; * pure functions that react to player moves
; * change detection that sends update messages to players
; * main loop that keeps track of and updates state

(defn- game-loop [game [recv-a send-a] [recv-b send-b]]
  (let [target-position (:target @game)]
    (go-loop []
      (let [[msg chan] (alts! [recv-a recv-b])
            player (if (= chan recv-a) :p1 :p2)
            other-player (the-other player)]
        (if msg
          (do
            (log/info "Received " msg "from" player)
            (when (:move msg)
              ; TODO: add sanity check that the new position is 1 away from previous
              (let [player-pos (:move msg)
                    other-pos (get-in @game [other-player :position])]
                (log/debug player player-pos other-player other-pos)
                (if (= player-pos other-pos)
                  (do
                    (log/info "Collision, randomizing positions")
                    (swap! game assoc-in [player :position] (place-player-randomly @game player))
                    (swap! game assoc-in [other-player :position] (place-player-randomly @game other-player))
                    (>! (if (= chan recv-a) send-a send-b) {:position          (get-in @game [player :position])
                                                            :opponent-position (get-in @game [other-player :position])})
                    (>! (if (= chan recv-a) send-b send-a) {:position          (get-in @game [other-player :position])
                                                            :opponent-position (get-in @game [player :position])}))
                  (if (some #(= player-pos %) (:jumpers @game))
                    ; handle jumper
                    (do
                      (log/info player "hit a jumper")
                      (swap! game assoc-in [player :position] (place-player-randomly @game player))
                      (swap! game update :jumpers (fn [jumpers] (remove #(= player-pos %) jumpers)))
                      (>! (if (= chan recv-a) send-a send-b) {:position (get-in @game [player :position])
                                                              :jumpers  (get-in @game [:jumpers])})
                      (>! (if (= chan recv-a) send-a send-b) {:opponent-position (get-in @game [other-player :position])
                                                              :jumpers           (get-in @game [:jumpers])})
                      )
                    (if (= player-pos target-position)
                      (do
                        (log/info player "wins")
                        (>! (if (= player :p1) send-a send-b) {:result :win})
                        (>! (if (= player :p1) send-b send-a) {:result :lose}))
                      (do
                        (swap! game assoc-in [player :position] (:move msg))
                        (>! (if (= chan recv-a) send-b send-a) {:opponent-position (:move msg)})))))))
            ; (log/debug "after" player (get-in @game [player :position]) other-player (get-in @game [other-player :position]))
            (recur))
          ((log/info "cleaning up pair")
            (close! send-a)
            (close! send-b)))))
    (log/info "Exiting game loop")))

(defn- make-game []
  (let [width 5
        height 5
        target-position [(quot width 2) (dec height)]
        p1-position [0 0]
        p2-position [(dec width) 0]
        jumpers [(place-randomly [width height] [p1-position p2-position target-position])]
        throwers [(place-randomly [width height] (concat jumpers [p1-position p2-position target-position]))]
        maze (maze/generate width height target-position)]
    {:maze     maze
     :p1       {:position p1-position}
     :p2       {:position p2-position}
     :jumpers  jumpers
     :throwers throwers
     :target   target-position}))

(defn- render-game-state [game player]
  {:maze              (:maze game)
   :position          (get-in game [player :position])
   :opponent-position (get-in game [(the-other player) :position])
   :jumpers           (get-in game [:jumpers])
   :throwers          (get-in game [:throwers])
   :target            (:target game)})

(defn handle-game [[recv-a send-a] [recv-b send-b]]
  (let [game (make-game)]
    (go
      (>! send-a (render-game-state game :p1))
      (>! send-b (render-game-state game :p2)))
    (game-loop (atom game) [recv-a send-a] [recv-b send-b])))
