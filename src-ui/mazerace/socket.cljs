(ns mazerace.socket
  (:require-macros [cljs.core.async.macros :refer (go go-loop)])
  (:require [cljs.core.async :refer (<! >! chan close!)]
            [reagent.core :as r]
            [mazerace.log :as log]))

(defonce connection-state (r/atom {:connecting true}))

(defn- prepare-url [path]
  (let [loc (.-location js/window)]
    (str (if (= "https:" (.-protocol loc))
           "wss" "ws")
         "://"
         (.-host loc)
         path)))

(defn connect-socket [path]
  (let [ws (js/WebSocket. (prepare-url path))
        send-ch (chan)
        recv-ch (chan)]
    (set! (.-onopen ws) (fn []
                          (log/info "connection opened")
                          (swap! connection-state assoc :connected true :connecting false)))
    (set! (.-onclose ws) (fn []
                           (log/info "connection close")
                           (swap! connection-state assoc :connected false :connecting false)))
    (set! (.-onmessage ws) (fn [e]
                             (try
                               (let [data (js->clj (js/JSON.parse (aget e "data")) :keywordize-keys true)]
                                 (go (>! recv-ch data)))
                               (catch js/Object e
                                 (log/info (str "Error parsing incoming message " e))
                                 (close! recv-ch)))))
    (go-loop []
             (if-let [msg (<! send-ch)]
               (do
                 (.send ws (js/JSON.stringify (clj->js msg)))
                 (recur))
               (.close ws)))
    [send-ch recv-ch]))

(defn state []
  @connection-state)