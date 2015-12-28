(ns mazerace.socket
  (:require [clojure.tools.logging :as log]
            [clojure.core.async :refer [close! <! >! chan go go-loop]]
            [clojure.data.json :as json]
            [org.httpkit.server :as hs]
            [mazerace.connectivity :as conn]))

(defn- receive-handler [receive-ch on-close]
  (fn [msg]
    (try
      (let [data (json/read-str msg :key-fn keyword)]
        (go (>! receive-ch data)))
      (catch Exception e
        (log/error "Error parsing input message" e)
        (on-close)))))

(defn- send-handler [chn send-ch on-close]
  (go-loop []
    (let [msg (<! send-ch)]
      (if msg
        (do
          (try
            (log/debug "->" msg)
            (hs/send! chn (json/json-str msg))
            (catch Exception e
              (log/error "Error sending data to WS" e)
              (on-close)))
          (recur))
        (do
          (log/info "shutting down channel")
          (on-close))))))

(defn socket-handler [req]
  (hs/with-channel req chn
                   (log/debug "New connection")
                   (let [receive-ch (chan)
                         send-ch (chan)
                         cleanup! (fn [] (hs/close chn) (close! receive-ch))]
                     (hs/on-close chn (fn [_] (close! receive-ch)))
                     (hs/on-receive chn (receive-handler receive-ch cleanup!))

                     (send-handler chn send-ch cleanup!)
                     ; TODO: send game parameters along with channels
                     (conn/handle-connection receive-ch send-ch))))