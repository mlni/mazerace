(ns mazerace.solver
  (:require [clojure.tools.logging :as log]
            [clojure.core.async :refer [>! <! go chan close! thread alts! timeout go-loop alt!]]))

(defn has-wall? [maze [x y] direction]
  (let [WALLS {:up 1 :right 2 :down 4 :left 8}]
    (not= 0 (bit-and (get-in maze [y x]) (get WALLS direction)))))

(defn exits [maze [x y]]
  (let [deltas {:left [-1 0] :right [1 0] :down [0 1] :up [0 -1]}
        width (count (first maze))
        height (count maze)]
    (for [[dir [dx dy]] deltas
          :let [xx (+ x dx) yy (+ y dy)]
          :when (and (<= 0 xx (dec width))
                     (<= 0 yy (dec height))
                     (not (has-wall? maze [x y] dir)))]
      [xx yy])))

(defn find-shortest-path [maze from to]
  (log/debug "find-path" from to)
  (letfn [(step [paths visited]
            (mapcat (fn [path]
                      (for [exit (exits maze (last path))
                            :when (not (contains? visited exit))]
                        (conj path exit)))
                    paths))
          (finished? [path] (= (last path) to))]
    (let [paths [[from]]
          visited (hash-set from)]
      (loop [paths paths visited visited]
        (if (empty? paths)
          nil                                               ; no path found
          (let [finished (filter finished? paths)]
            (if (seq finished)
              (first finished)
              (let [paths (step paths visited)]
                (recur paths (into visited (map last paths)))))))))))

(defn start-solver-loop [game send-ch]
  (go
    (<! (timeout 5000))                                     ; pretend to solve the maze
    (loop []
      (let [[_ ch] (alts! [send-ch (timeout 500)])
            path (:path @game)]
        (if (not= ch send-ch)
          (do
            (when (seq path)
              (swap! game assoc :path (rest path))
              (>! send-ch {:move (first path)}))
            (recur))
          (log/debug "Exiting solver loop"))))))

(defn computer-player [[recv-ch send-ch]]
  (let [game (atom {})]
    (start-solver-loop game send-ch)
    (go
      (loop []
        (let [msg (<! recv-ch)]
          (log/debug "Cmp got" msg)
          (if msg
            (do
              (when (:maze msg)
                (reset! game msg))
              (when (:position msg)
                (swap! game assoc
                       :position (:position msg)
                       :path (find-shortest-path (:maze @game) (:position msg) (:target @game))))
              (when (:result game)
                (swap! game assoc :path nil))
              (when (not (:result game))
                (recur)))
            (do
              (log/debug "Closing receive loop")
              (swap! game assoc :path nil)
              (close! recv-ch)
              (close! send-ch))))))))
