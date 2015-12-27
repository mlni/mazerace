(ns mazerace.maze
  (:require [clojure.pprint :refer [pprint]]))

(defn- unvisited [maze visited [x y]]
  (let [deltas [[-1 0] [1 0] [0 -1] [0 1]]]
    (filter (fn [cell]
              (not (contains? visited cell)))
            (for [[dx dy] deltas
                  :let [xx (+ x dx) yy (+ y dy)]
                  :when (and (>= xx 0) (>= yy 0) (< xx (count (first maze))) (< yy (count maze)))]
              [xx yy]))))

(defn- direction [[x1 y1] [x2 y2]]
  (if (not= x1 x2)
    (if (pos? (- x2 x1)) :r :l)
    (if (pos? (- y2 y1)) :d :u)))

(defn- reverse-direction [dir]
  (get {:l :r :r :l :u :d :d :u} dir))

(defn- connect [maze [this-x this-y] [other-x other-y]]
  (let [direction (direction [this-x this-y] [other-x other-y])]
    (-> maze
        (assoc-in [this-y this-x direction] false)
        (assoc-in [other-y other-x (reverse-direction direction)] false))))

(defn- generate-maze [width height start]
  (let [walls (into [] (for [y (range height)]
                         (into [] (for [x (range width)]
                                    {:u true :d true :l true :r true}))))
        visited (hash-set)
        path (list start)]
    (loop [path path visited visited walls walls]
      (if (empty? path)
        walls
        (let [current-cell (first path)
              unvisited-neighbors (unvisited walls visited current-cell)]
          (if (empty? unvisited-neighbors)
            (recur (rest path) (conj visited current-cell) walls)
            (let [neighbor (rand-nth unvisited-neighbors)]
              (recur (conj path neighbor)
                     (conj visited current-cell)
                     (connect walls current-cell neighbor)))))))))

(defn- compact
  "Represent each cell in the maze as an integer, where lower 4 bits represent
  presence of walls respectively on top, right, bottom and left side."
  [maze]
  (letfn [(bit [x] (if x 1 0))
          (encode [cell]
            (bit-or (bit-shift-left (bit (:u cell)) 0)
                    (bit-shift-left (bit (:r cell)) 1)
                    (bit-shift-left (bit (:d cell)) 2)
                    (bit-shift-left (bit (:l cell)) 3)))]
    (into []
          (map (fn [row]
                 (into [] (map encode row))) maze))))

(defn generate [width height target-position]
  (let [maze (generate-maze width height target-position)]
    ; remove a couple of walls to make it easier?
    (compact maze)))

(defn visualize [walls]
  (doseq [row walls]
    (println (apply str (map (fn [cell]
                               (str "+" (if (:u cell) "-" " ") "+")) row)))
    (println (apply str (map (fn [cell]
                               (str (if (:l cell) "|" " ") " " (if (:r cell) "|" " "))) row)))
    (println (apply str (map (fn [cell]
                               (str "+" (if (:d cell) "-" " ") "+")) row)))))

