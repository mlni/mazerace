(ns mazerace.game
  (:require [clojure.tools.logging :as log]
            [clojure.core.async :refer [>! <! >!! <!! go chan close! thread alts! alts!! timeout go-loop]]
            [mazerace.maze :as maze]
            [mazerace.maze :as maze]))


(defn- the-other [player]
  (if (= :p1 player) :p2 :p1))

(defn- render-game-state [game player]
  {:maze              (:maze game)
   :position          (get-in game [player :position])
   :opponent-position (get-in game [(the-other player) :position])
   :result            (get-in game [player :result])
   :jumpers           (get-in game [:jumpers])
   :throwers          (get-in game [:throwers])
   :target            (:target game)})

(defn- abs [x]
  (max x (- x)))

(defn- without [val]
  (fn [vals]
    (remove #(= val %) vals)))

(defn- find-differences [a b]
  (let [keys (apply sorted-set (concat (keys a) (keys b)))]
    (reduce (fn [r [k v]] (assoc r k v))
            nil
            (keep identity
                  (map (fn [k]
                         (when (not= (get a k) (get b k))
                           [k (get b k)]))
                       keys)))))

(defn- place-randomly [[width height] avoid-positions]
  (loop []
    (let [rand-x (rand-int width)
          rand-y (rand-int height)]
      (if (some #(= [rand-x rand-y] %) avoid-positions)
        (recur)
        [rand-x rand-y]))))

(defn- place-player-randomly [game player]
  (let [other-pos (get-in game [(the-other player) :position])
        target (get-in game [:target])
        avoid (concat (get-in game [:jumpers])
                      (get-in game [:throwers])
                      [other-pos target])
        width (count (get-in game [:maze 0]))
        height (count (get-in game [:maze]))]
    (place-randomly [width height] avoid)))

(defn- handle-player-collision [game player]
  (let [player-pos (get-in game [player :position])]
    (when (= player-pos (get-in game [(the-other player) :position]))
      (do
        (log/info "Collision, randomizing positions")
        (-> game
            (assoc-in [player :position] (place-player-randomly game player))
            (assoc-in [(the-other player) :position] (place-player-randomly game (the-other player))))))))

(defn- handle-jumpers [game player]
  (let [player-pos (get-in game [player :position])]
    (when (some #(= player-pos %) (:jumpers game))
      (-> game
          (assoc-in [player :position] (place-player-randomly game player))
          (update :jumpers (without player-pos))))))

(defn- handle-throwers [game player]
  (let [player-pos (get-in game [player :position])
        opponent (the-other player)]
    (when (some #(= player-pos %) (:throwers game))
      (-> game
          (assoc-in [opponent :position] (place-player-randomly game opponent))
          (update :throwers (without player-pos))))))

(defn- handle-finish [game player]
  (let [player-pos (get-in game [player :position])]
    (when (= player-pos (get game :target))
      (-> game
          (assoc-in [player :result] :win)
          (assoc-in [(the-other player) :result] :lose)))))

(defn- player-move [game player player-pos]
  (let [[prev-x prev-y] (get-in game [player :position])
        [new-x new-y] player-pos
        distance (+ (abs (- prev-x new-x))
                    (abs (- prev-y new-y)))]
    (log/debug player player-pos)
    (let [game' (if (> distance 1)
                  (do                                       ; detect obvious hacking attempts
                    (log/info "Move jumps more than 1 step" player [prev-x prev-y] player-pos)
                    game)
                  (assoc-in game [player :position] player-pos))
          pipeline [handle-player-collision
                    handle-jumpers
                    handle-throwers
                    handle-finish]
          final-state (reduce (fn [game step] (or (step game player) game))
                              game' pipeline)]
      (let [p1-update (find-differences
                        (render-game-state (if (= player :p1) game' game) :p1)
                        (render-game-state final-state :p1))
            p2-update (find-differences
                        (render-game-state (if (= player :p2) game' game) :p2)
                        (render-game-state final-state :p2))]
        (log/debug "p1 diff" p1-update)
        (log/debug "p2 diff" p2-update)
        [final-state p1-update p2-update]))))

(defn- game-loop [game [recv-a send-a] [recv-b send-b]]
  (go-loop []
    (let [[msg chan] (alts! [recv-a recv-b])
          player (if (= chan recv-a) :p1 :p2)]
      (if msg
        (do
          (log/info "Received " msg "from" player)
          (when (:move msg)
            (let [[game' p1-update p2-update] (player-move @game player (:move msg))]
              (when p1-update
                (>! send-a p1-update))
              (when p2-update
                (>! send-b p2-update))
              (reset! game game')))
          (recur))
        ((log/info "cleaning up pair")
          (close! send-a)
          (close! send-b)))))
  (log/info "Exiting game loop"))

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

(defn start-game [[recv-a send-a] [recv-b send-b]]
  (let [game (make-game)]
    (go
      (>! send-a (render-game-state game :p1))
      (>! send-b (render-game-state game :p2)))
    (game-loop (atom game) [recv-a send-a] [recv-b send-b])))
