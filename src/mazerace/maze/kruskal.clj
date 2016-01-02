(ns mazerace.maze.kruskal
  (:require [mazerace.maze.core :as c]))

(defn- opposite [dir]
  (get {:l :r :u :d} dir))

(defn- cell-from [x y direction]
  (let [deltas {:l [-1 0] :u [0 -1]}
        [dx dy] (get deltas direction)]
    [(+ x dx) (+ y dy)]))

(defn- merge-sets [maze [x1 y1] dir [x2 y2] set1 set2]
  (let [merged-set (into set1 set2)
        maze-wo-wall (-> maze
                         (assoc-in [y1 x1 dir] false)
                         (assoc-in [y2 x2 (opposite dir)] false))]
    (reduce (fn [maze [x y]] (assoc-in maze [y x :set] merged-set))
            maze-wo-wall
            merged-set)))

(defn generate
  "Generate a maze using Kruskal's algorithm."
  [width height]
  (let [maze (into [] (for [y (range height)]
                        (into [] (for [x (range width)]
                                   {:l true :r true :u true :d true :set (sorted-set [x y])}))))
        walls (for [x (range width)
                    y (range height)
                    dir [:l :u]
                    :when (or (and (= dir :u) (> y 0))
                              (and (= dir :l) (> x 0)))]
                [x y dir])]
    (c/compact
      (reduce (fn [maze [x y dir]]
                (let [[x2 y2] (cell-from x y dir)
                      set1 (get-in maze [y x :set])
                      set2 (get-in maze [y2 x2 :set])]
                  (if (= set1 set2)
                    maze
                    (merge-sets maze [x y] dir [x2 y2] set1 set2))))
              maze
              (shuffle walls)))))
