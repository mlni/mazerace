(ns mazerace.game
  (:require-macros [cljs.core.async.macros :refer (go go-loop)])
  (:require [reagent.core :as r]
            [mazerace.log :as log]
            [mazerace.input :as input]
            [mazerace.socket :as ws]
            [cljs.core.async :refer (<! >! put! chan close!)]))

(defonce game (r/atom nil))

(declare dispatch)

(defn- direction [[ox oy] [nx ny]]
  (cond (< ox nx) :right
        (> ox nx) :left
        (> oy ny) :up
        :else :down))

(defn- within-boundary [maze x y]
  (and (>= x 0) (>= y 0) (< x (count (first maze))) (< y (count maze))))

(defn- has-wall? [cell dir]
  (let [bit (bit-shift-left 1 (get {:up 0 :right 1 :down 2 :left 3} dir))]
    (not (zero? (bit-and cell bit)))))

(defn handle-move [game [dir]]
  (let [[x y] (:position game)
        maze (:maze game)
        [dx dy] (get {:up [0 -1] :down [0 1] :left [-1 0] :right [1 0]} dir)
        xx (+ x dx)
        yy (+ y dy)]
    (when (and (within-boundary maze xx yy)
               (not (has-wall? (get-in maze [y x]) dir)))
      (log/info (str "moving to " xx " " yy))
      [(-> game
           (assoc :position [xx yy])
           (assoc :direction dir))
       {:move [xx yy]}])))

(defn- on-maze [game data]
  (assoc game :maze (:maze data)
              :state :playing
              :position nil
              :opponent-position nil
              :direction :down
              :opponent-direction :down))

(defn- on-opponent-move [game data]
  (-> game
      (assoc :opponent-position (:opponent-position data))
      (assoc :opponent-direction (direction (or (:opponent-position game) (:opponent-position data))
                                            (:opponent-position data)))))

(defn- update-field-fn [key]
  (fn [game data]
    (assoc game key (get data key))))

(defn handle-server-message [game [data]]
  (let [pipeline (concat [[:maze on-maze]
                          [:opponent-position on-opponent-move]]
                         (map (fn [key] [key (update-field-fn key)])
                              [:position :jumpers :throwers :result :target]))
        game (reduce (fn [game [key handler-fn]]
                       (if (contains? data key)
                         (handler-fn game data)
                         game))
                     game pipeline)]
    [game]))


(defn handle-start-game [game]
  (let [[send-ch recv-ch] (ws/connect-socket "/ws")
        send! (fn [msg] (go (>! send-ch msg)))
        stop! (fn [] (close! send-ch))]
    (input/register-handler (fn [dir] (dispatch :move dir)))
    (go (loop [_ nil]
          (if-let [data (<! recv-ch)]
            (recur (dispatch :server-event data))
            (close! send-ch))))
    [(assoc game
       :state :connecting
       :send-fn send!
       :stop-fn stop!)]))

(defn handle-play-against-computer [game]
  (when (= :connecting (:state game))
    [game {:opponent "computer"}]))

(defn handle-navigation [game [page]]
  (when-let [stop! (:stop-fn game)]
    (stop!))                                                ; hang up when navigating away from /play

  (if (= :play page)
    (handle-start-game game)
    [(assoc game :state page)]))

(def event-handlers
  {:start-game            handle-start-game
   :play-against-computer handle-play-against-computer
   :move                  handle-move
   :server-event          handle-server-message
   :navigate              handle-navigation})

(defn dispatch [event-name & args]
  (if-let [handler (get event-handlers event-name)]
    (let [[state message] (handler @game args)]
      (when state
        (reset! game state))
      (when (and message (:send-fn state))
        ((:send-fn state) message)))
    (log/info "Unknown event in dispatch" event-name)))

(defn game-state []
  @game)
