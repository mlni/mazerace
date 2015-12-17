(ns mazerace.main
  (:require-macros [cljs.core.async.macros :as asyncm :refer (go go-loop)])
  (:require [cljs.core.async :as async :refer (<! >! put! chan)]
            [reagent.core :as r]))

(defonce game (r/atom nil))

(defn log [msg]
  (js/console.log (.getTime (js/Date.)) msg))

(def send-ch (chan))

(defn- prepare-url [path]
  (let [loc (.-location js/window)]
    (str (if (= "https" (.-protocol loc))
           "wss" "ws")
         "://"
         (.-host loc)
         path)))

(defn connect-socket []
  (let [ws (js/WebSocket. (prepare-url "/ws"))]
    (set! (.-onopen ws) (fn [] (log "connection opened")))
    (set! (.-onclose ws) (fn [] (log "connection close")))
    (set! (.-onmessage ws) (fn [e]
                             (let [data (js->clj (js/JSON.parse (.-data e))
                                                 :keywordize-keys true)]
                               (js/console.log data)
                               (when (:maze data)
                                 (reset! game {:maze              (:maze data)
                                               :target            (:target data)
                                               :position          (:position data)
                                               :opponent-position (:opponent-position data)}))
                               (when (:position data)
                                 (swap! game assoc :position (:position data)))
                               (when (:opponent-position data)
                                 (swap! game assoc :opponent-position (:opponent-position data)))
                               (when (:result data)
                                 (reset! game {:result (:result data)})))))
    (go-loop []
             (when-let [msg (<! send-ch)]
               (.send ws (js/JSON.stringify (clj->js msg)))
               (recur)))))

(log "Starting")
(connect-socket)

(defn send! [msg]
  (go (>! send-ch msg)))

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
      (swap! game assoc :position [xx yy])
      (send! {:move [xx yy]}))))

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
        [pos-x pos-y] (:position @game)
        [opp-x opp-y] (:opponent-position @game)
        [target-x target-y] (:target @game)]
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
               (if (and (= opp-x cellnum)
                        (= opp-y rownum))
                 "X"
                 (if (and (= target-x cellnum)
                          (= target-y rownum))
                   "V"
                   "\u00A0")))])]))]))


(defn page []
  [:h1 (if (:result @game)
         (:result @game)
         (if (:maze @game)
           "Race!"
           "Wait for opponent..."))
   (when (:maze @game)
     [render-maze])])

(r/render [page]
          (js/document.getElementById "app"))