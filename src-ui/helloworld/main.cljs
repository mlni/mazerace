(ns helloworld.main
  (:require-macros [cljs.core.async.macros :as asyncm :refer (go go-loop)])
  (:require [cljs.core.async :as async :refer (<! >! put! chan)]
            [reagent.core :as r]))

(defonce game (r/atom nil))

(defn log [msg]
  (js/console.log (.getTime (js/Date.)) msg))

(defn connect-socket []
  (let [ws (js/WebSocket. "ws://localhost:3000/ws")]
    (set! (.-onopen ws) (fn [] (log "connection opened")))
    (set! (.-onclose ws) (fn [] (log "connection close")))
    (set! (.-onmessage ws) (fn [e]
                             (let [data (js->clj (js/JSON.parse (.-data e)) :keywordize-keys true)]
                               (js/console.log data)
                               (when data
                                 (reset! game {:maze data :position [0 0]})))))))

(log "Starting")
(connect-socket)

(defn- within-boundary [maze x y]
  (and (>= x 0) (>= y 0) (< x (count (first maze))) (< y (count maze))))

(defn attempt-move [dir]
  (let [[x y] (:position @game)
        maze (:maze @game)
        [dx dy] (get {:up [0 -1] :down [0 1] :left [-1 0] :right [1 0]} dir)
        xx (+ x dx)
        yy (+ y dy)
        wall (get {:up :u :down :d :left :l :right :r} dir)]
    (when (and (within-boundary maze xx yy)
               (not (get-in maze [y x wall])))
      (log (str "moving to " xx " " yy))
      (swap! game assoc :position [xx yy]))))

(set! (.-onkeydown js/document)
      (fn [e]
        (let [dir (condp = (.-keyCode e) 38 :up
                                         40 :down
                                         37 :left
                                         39 :right
                                         :none)]
          (log (str "direction " dir))
          (when (not= dir :none)
            (attempt-move dir)))))

(defn render-maze []
  (let [maze (:maze @game)
        [pos-x pos-y] (:position @game)]
    [:table {:className "maze"}
     (doall
       (for [rownum (range (count maze)) :let [row (nth maze rownum)]]
         ^{:key rownum}
         [:tr {:className "row"}
          (for [cellnum (range (count row))
                :let [cell (nth row cellnum)
                      style (str (when (:l cell) "left ")
                                 (when (:r cell) "right ")
                                 (when (:u cell) "top ")
                                 (when (:d cell) "bottom "))]]
            ^{:key cellnum}
            [:td {:className style}
             (if (and (= pos-x cellnum)
                      (= pos-y rownum))
               "O"
               "\u00A0")])]))]))


(defn page []
  [:div "hello there"]
  (when @game
    [render-maze]))

(r/render [page]
          (js/document.getElementById "app"))