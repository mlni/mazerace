(ns mazerace.ui.svg
  (:require [mazerace.game :as game])
  (:require-macros [reagent.ratom :refer [reaction]]))

(def size 10)

(defn- render-mouse [x y direction color]
  (let [rotate (get {:left 90 :up 180 :right 270 :down 0} direction 0)]
    [:svg {:width   (dec size)
           :height  (dec size)
           :x       (+ 0.5 (* x size))
           :y       (+ 0.5 (* y size))
           :viewBox "0 0 100 100"}
     [:g {:stroke "black" :stroke-width 3 :fill color :transform (str "rotate(" rotate ", 50, 50)")}
      [:path {:d "M35 80
        Q 50 115 65 80
        A 10 10 -45 1 0 70 60
        C 80 5 20 5 30 60
        A 10 10 45 1 0 35 80
        Z
        M 70 60
        A 10 10 0 0 0 60 65
        M 30 60
        A 10 10 0 0 1 40 65"}]
      [:circle {:cx 45 :cy 80 :r 2 :fill "black"}]
      [:circle {:cx 55 :cy 80 :r 2 :fill "black"}]
      [:path {:d "M50 19 C 46 15 54 5 50 1"}]]]))

(defn- render-cheese [x y]
  [:svg {:width   (dec size)
         :height  (dec size)
         :x       (+ 0.5 (* x size))
         :y       (* y size)
         :viewBox "0 0 100 100"}
   [:g {:stroke "black" :stroke-width 2 :fill "yellow"}
    [:path {:d "M 5 50
                L 95 50
                L 95 85
                L 5 85
                Z"}]
    [:path {:d "M 5 50
                L 66 25
                A 50 50 0 0 1 95 50
                Z"}]]
   [:g {:stroke "black" :stroke-width 2 :fill "white"}
    [:circle {:cx "25" :cy "65" :r "6"}]
    [:circle {:cx "55" :cy "75" :r "6"}]
    [:circle {:cx "80" :cy "60" :r "6"}]]])

(defn- render-portal [x y color]
  [:g {:stroke "gray" :stroke-width "0" :fill "gray"}       ;shadow
   [:ellipse {:cx (+ (* x size) (quot size 2))
              :cy (+ (* y size) (quot size 2) +0.5)
              :rx (dec (quot size 2))
              :ry (dec (quot size 2.5))}]
   [:g {:stroke "black" :stroke-width ".25" :fill color}
    [:ellipse {:cx (+ (* x size) (quot size 2))
               :cy (+ (* y size) (quot size 2) -0.5)
               :rx (dec (quot size 2))
               :ry (dec (quot size 2.5))}]]])

(defn- has-wall? [cell dir]
  (let [bit (bit-shift-left 1 (get {:up 0 :right 1 :down 2 :left 3} dir))]
    (not (zero? (bit-and cell bit)))))

(defn- render-cell [cell rownum cellnum size]
  ^{:key (str rownum "-" cellnum)}
  [:g {}
   (when (has-wall? cell :up)
     [:line {:x1 (* cellnum size)
             :y1 (* rownum size)
             :x2 (* (inc cellnum) size)
             :y2 (* rownum size)}])
   (when (has-wall? cell :right)
     [:line {:x1 (* (inc cellnum) size)
             :y1 (* rownum size)
             :x2 (* (inc cellnum) size)
             :y2 (* (inc rownum) size)}])
   (when (has-wall? cell :down)
     [:line {:x1 (* cellnum size)
             :y1 (* (inc rownum) size)
             :x2 (* (inc cellnum) size)
             :y2 (* (inc rownum) size)}])
   (when (has-wall? cell :left)
     [:line {:x1 (* cellnum size)
             :y1 (* rownum size)
             :x2 (* cellnum size)
             :y2 (* (inc rownum) size)}])])

(defn- render-maze []
  (let [maze (reaction (:maze (game/game-state)))]
    (fn []
      (let [width (count (first @maze))
            height (count @maze)]
        [:g {:stroke "black" :stroke-width "1" :fill "white" :stroke-linecap "round"}
         (doall
           (for [rownum (range height)
                 :let [row (nth @maze rownum)]
                 cellnum (range width)
                 :let [cell (nth row cellnum)]]
             (render-cell cell rownum cellnum size)))]))))

(defn render-jumpers []
  (let [jumpers (reaction (:jumpers (game/game-state)))]
    (fn []
      [:g (doall
            (for [[x y] @jumpers]
              ^{:key (str "jmp" x "-" y)}
              [render-portal x y "white"]))])))

(defn render-throwers []
  (let [throwers (reaction (:throwers (game/game-state)))]
    (fn []
      [:g (doall
            (for [[x y] @throwers]
              ^{:key (str "thr" x "-" y)}
              [render-portal x y "black"]))])))

(defn render-target []
  (let [target (reaction (:target (game/game-state)))]
    (fn []
      [:g (let [[x y] @target]
            [render-cheese x y])])))

(defn render-opponent []
  (let [position (reaction (:opponent-position (game/game-state)))
        direction (reaction (:opponent-direction (game/game-state)))]
    (fn []
      [:g (let [[x y] @position]
            [render-mouse x y @direction "gray"])])))

(defn render-player []
  (let [position (reaction (:position (game/game-state)))
        direction (reaction (:direction (game/game-state)))]
    (fn []
      [:g {:stroke "black" :stroke-width "1" :fill "white"}
       (let [[x y] @position]
         [render-mouse x y @direction "white"])])))

(defn render-game []
  (let [maze (reaction (:maze (game/game-state)))]
    (fn []
      (let [width (count (first @maze))
            height (count @maze)]
        [:svg {:className "maze-svg"
               :viewBox   (str "-1 -1 " (+ 2 (* width size)) " " (+ 2 (* height size)))}
         [render-maze]
         [render-jumpers]
         [render-throwers]
         [render-target]
         [render-opponent]
         [render-player]]))))
