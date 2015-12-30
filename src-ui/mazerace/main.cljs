(ns mazerace.main
  (:require-macros [cljs.core.async.macros :refer (go go-loop)])
  (:require [cljs.core.async :refer (<! >! put! chan close!)]
            [reagent.core :as r]
            [mazerace.log :as log]
            [mazerace.socket :as ws]
            [mazerace.input :as input]
            [mazerace.game :as game]
            [mazerace.ui.pages :as pages]
            [mazerace.window :as window]))

(log/info "Starting ...")

(let [[send-ch recv-ch] (ws/connect-socket "/ws")
      send! (fn [msg] (go (>! send-ch msg)))]
  (input/register-handler (fn [dir] (game/handle-move dir send!)))
  (go-loop []
           (if-let [data (<! recv-ch)]
             (do
               (game/handle-server-message data)
               (recur))
             (close! send-ch))))

(log/info "Rendering")

(r/render [pages/index]
          (.getElementById js/document "app"))

(window/handle-resize!)

(log/info "Started")
