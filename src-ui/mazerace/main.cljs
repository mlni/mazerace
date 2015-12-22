(ns mazerace.main
  (:require-macros [cljs.core.async.macros :as asyncm :refer (go go-loop)])
  (:require [cljs.core.async :as async :refer (<! >! put! chan)]
            [reagent.core :as r]))

(defonce connection-state (r/atom {:connecting true}))
(defonce game (r/atom nil))

(defn log [msg]
  (js/console.log (.getTime (js/Date.)) msg))

(def send-ch (chan))

(defn- prepare-url [path]
  (let [loc (.-location js/window)]
    (str (if (= "https:" (.-protocol loc))
           "wss" "ws")
         "://"
         (.-host loc)
         path)))

(defn- direction [[ox oy] [nx ny]]
  (cond (< ox nx) :right
        (> ox nx) :left
        (> oy ny) :up
        :else :down))

(defn connect-socket []
  (let [ws (js/WebSocket. (prepare-url "/ws"))]
    (set! (.-onopen ws) (fn []
                          (log "connection opened")
                          (swap! connection-state assoc :connected true :connecting false)))
    (set! (.-onclose ws) (fn []
                           (log "connection close")
                           (swap! connection-state assoc :connected false :connecting false)))
    (set! (.-onmessage ws) (fn [e]
                             (let [data (js->clj (js/JSON.parse (.-data e)) :keywordize-keys true)]
                               ; TODO: clean up state management here
                               (when (:maze data)
                                 (reset! game {:maze              (:maze data)
                                               :target            (:target data)
                                               :position          (:position data)
                                               :opponent-position (:opponent-position data)
                                               :jumpers           (:jumpers data)
                                               :throwers          (:throwers data)}))
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
                                 (reset! game {:result (:result data)})))))
    (go-loop []
             (when-let [msg (<! send-ch)]
               ; TODO: add sending keepalives if no messages during timeout period
               (.send ws (js/JSON.stringify (clj->js msg)))
               (recur)))))

(log "Starting")
(connect-socket)

(defn send! [msg]
  (go (>! send-ch msg)))

(defn- within-boundary [maze x y]
  (and (>= x 0) (>= y 0) (< x (count (first maze))) (< y (count maze))))

(defn- has-wall? [cell dir]
  (let [bit (bit-shift-left 1 (get {:up 0 :right 1 :down 2 :left 3} dir))]
    (not (zero? (bit-and cell bit)))))

(defn attempt-move! [dir]
  (let [[x y] (:position @game)
        maze (:maze @game)
        [dx dy] (get {:up [0 -1] :down [0 1] :left [-1 0] :right [1 0]} dir)
        xx (+ x dx)
        yy (+ y dy)]
    (when (and (within-boundary maze xx yy)
               (not (has-wall? (get-in maze [y x]) dir)))
      (log (str "moving to " xx " " yy))
      (swap! game assoc :position [xx yy])
      (swap! game assoc :direction dir)
      (send! {:move [xx yy]}))))

(set! (.-onkeydown js/document)
      (fn [e]
        (let [dir (condp = (.-keyCode e) 38 :up
                                         87 :up
                                         40 :down
                                         83 :down
                                         37 :left
                                         65 :left
                                         39 :right
                                         68 :right
                                         :none)]
          (when (not= dir :none)
            (attempt-move! dir)))))

(defn- render-cell [game cell]
  (let [symbols [[:position "O"]
                 [:opponent-position "X"]
                 [:target "V"]
                 [:jumpers "\u25EF"]
                 [:throwers "\u2B24"]]
        default "\u00A0"]
    (or (first (drop-while nil?
                           (map (fn [[key symbol]]
                                  (when (or (= (get game key) cell)
                                            (some (fn [x]
                                                    (= cell x)) (get game key)))
                                    symbol)) symbols)))
        default)))

(defn render-maze-html []
  (let [maze (:maze @game)]
    [:table {:className "maze-html"}
     (doall
       (for [rownum (range (count maze)) :let [row (nth maze rownum)]]
         ^{:key rownum}
         [:tr {:className "row"}
          (doall
            (for [cellnum (range (count row))
                  :let [cell (nth row cellnum)
                        style (str (when (has-wall? cell :left) "left ")
                                   (when (has-wall? cell :right) "right ")
                                   (when (has-wall? cell :up) "top ")
                                   (when (has-wall? cell :down) "bottom "))]]
              ^{:key cellnum}
              [:td {:className style}
               (render-cell @game [cellnum rownum])]))]))]))

(defn- render-mouse [x y direction size color]
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

(defn- render-cheese [x y size]
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

(defn- render-portal [x y size color]
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

(defn- render-maze-svg []
  (let [maze (:maze @game)
        width (count (first maze))
        height (count maze)
        size 10]
    [:svg {:className "maze-svg"
           :viewBox   (str "-1 -1 " (+ 2 (* width size)) " " (+ 2 (* height size)))}
     [:g {:stroke "black" :stroke-width "1" :fill "white" :stroke-linecap "round"}
      (doall
        (for [rownum (range height)
              :let [row (nth maze rownum)]
              cellnum (range width)
              :let [cell (nth row cellnum)]]
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
                     :y2 (* (inc rownum) size)}])]))]
     [:g
      (doall
        (for [[x y] (:jumpers @game)]
          ^{:key (str "jmp" x "-" y)}
          [render-portal x y size "white"]))]
     [:g
      (doall
        (for [[x y] (:throwers @game)]
          ^{:key (str "thr" x "-" y)}
          [render-portal x y size "black"]))]
     [:g
      (let [[x y] (:target @game)]
        [render-cheese x y size])]
     [:g
      (let [[x y] (:opponent-position @game)]
        [render-mouse x y (:opponent-direction @game) size "gray"])]
     [:g {:stroke "black" :stroke-width "1" :fill "white"}
      (let [[x y] (:position @game)]
        [render-mouse x y (:direction @game) size "white"])]]))

(defn page []
  [:div.container
   [:h1 (if (:connecting @connection-state)
          "Connecting..."
          (if (not (:connected @connection-state))
            "Disconnected"
            (if (:result @game)
              (:result @game)
              (if (:maze @game)
                "Race!"
                "Waiting for an opponent..."))))]
   (when (and (:connected @connection-state) (:maze @game))
     [render-maze-svg])])

(r/render [page]
          (js/document.getElementById "app"))