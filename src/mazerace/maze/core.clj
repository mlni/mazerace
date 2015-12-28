(ns mazerace.maze.core)

(defn compact
  "Given a maze represented as a sequence of sequences of cells, where each cell
   is a map of walls in the form {:u true :l false :d true :right false},
   represent each cell in the maze as an integer, where lower 4 bits represent
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