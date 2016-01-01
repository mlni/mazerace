(ns mazerace.game
  (:require-macros [cljs.core.async.macros :refer (go go-loop)])
  (:require [reagent.core :as r]
            [mazerace.log :as log]
            [mazerace.input :as input]
            [mazerace.socket :as ws]
            [cljs.core.async :refer (<! >! put! chan close!)]))

(defonce game (r/atom nil))

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

(defn handle-move [dir send-fn!]
  (let [[x y] (:position @game)
        maze (:maze @game)
        [dx dy] (get {:up [0 -1] :down [0 1] :left [-1 0] :right [1 0]} dir)
        xx (+ x dx)
        yy (+ y dy)]
    (when (and (within-boundary maze xx yy)
               (not (has-wall? (get-in maze [y x]) dir)))
      (log/info (str "moving to " xx " " yy))
      (swap! game assoc :position [xx yy])
      (swap! game assoc :direction dir)
      (send-fn! {:move [xx yy]}))))

(defn handle-server-message [data]
  (when (:maze data)
    (reset! game {:maze              (:maze data)
                  :target            (:target data)
                  :position          (:position data)
                  :opponent-position (:opponent-position data)
                  :jumpers           (:jumpers data)
                  :throwers          (:throwers data)
                  :state             :playing}))
  (when (:position data)
    (swap! game assoc :position (:position data)))
  (when (:opponent-position data)
    (swap! game assoc :opponent-direction
           (direction (or (:opponent-position @game) (:opponent-position data))
                      (:opponent-position data)))
    (swap! game assoc :opponent-position (:opponent-position data)))
  (when (:jumpers data)
    (swap! game assoc :jumpers (:jumpers data)))
  (when (:throwers data)
    (swap! game assoc :throwers (:throwers data)))
  (when (:result data)
    (swap! game assoc :result (:result data))))

(defn game-state []
  @game)

(defn start-game! []
  (let [[send-ch recv-ch] (ws/connect-socket "/ws")
        send! (fn [msg] (go (>! send-ch msg)))]
    (input/register-handler (fn [dir] (handle-move dir send!)))
    (go (loop [_ nil]
          (if-let [data (<! recv-ch)]
            (recur (handle-server-message data))
            (close! send-ch))))
    (swap! game assoc :state :connecting)))
