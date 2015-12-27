(ns mazerace.socket
  (:require [clojure.tools.logging :as log]
            [clojure.core.async :refer [close! <! >! chan go go-loop]]
            [clojure.data.json :as json]
            [org.httpkit.server :as hs]
            [mazerace.connectivity :as conn]))

(defn socket-handler [req]
  (hs/with-channel req chn
                   (log/debug "New connection")
                   (let [receive-ch (chan)
                         send-ch (chan)
                         cleanup! (fn []
                                    (close! receive-ch)
                                    (hs/close chn))]
                     (hs/on-close chn (fn [_]
                                        (close! receive-ch)))
                     (hs/on-receive chn (fn [msg]
                                          (try
                                            (let [data (json/read-str msg :key-fn keyword)]
                                              (go (>! receive-ch data)))
                                            (catch Exception e
                                              (log/error "Error parsing input message" e)
                                              (cleanup!)))))
                     (go-loop []
                       (let [msg (<! send-ch)]
                         (if msg
                           (do
                             (try
                               (log/debug "->" msg)
                               (hs/send! chn (json/json-str msg))
                               (catch Exception e
                                 (log/error "Error sending data to WS" e)
                                 (cleanup!)))
                             (recur))
                           (do
                             (log/info "shutting down channel")
                             (cleanup!)))))
                     (conn/handle-connection receive-ch send-ch))))