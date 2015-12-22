(ns mazerace.core
  (:use org.httpkit.server
        (compojure [core :only [defroutes GET POST]]
                   [route :only [files not-found resources]]
                   [handler :only [site]]))
  (:require [ring.middleware.reload :as reload]
            [ring.util.response :as resp]
            [clojure.data.json :as json]
            [mazerace.connectivity :as conn]
            [mazerace.maze :as maze]
            [clojure.core.async :refer [close! <! >! chan go go-loop]]
            [clojure.tools.logging :as log])
  (:gen-class))


(defn push-handler [req]
  (with-channel req chn
                (log/debug "New connection")
                (let [receive-ch (chan)
                      send-ch (chan)
                      cleanup! (fn []
                                 (close! receive-ch)
                                 (close chn))]
                  (on-close chn (fn [_]
                                  (close! receive-ch)))
                  (on-receive chn (fn [msg]
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
                            (send! chn (json/json-str msg))
                            (catch Exception e
                              (log/error "Error sending data to WS" e)
                              (cleanup!)))
                          (recur))
                        (do
                          (log/info "shutting down channel")
                          (cleanup!)))))
                  (conn/handle-connection receive-ch send-ch))))

(defroutes app
           (GET "/" [] (resp/redirect "index.html"))
           (GET "/ws" [] push-handler)
           (resources "/" {:root "/public"})
           (not-found "Page not found"))

(defn -main [& args]
  (let [handler (-> #'app site reload/wrap-reload ring.middleware.keyword-params/wrap-keyword-params ring.middleware.params/wrap-params)
        port (Integer/parseInt (or (System/getenv "PORT") "3000"))]
    (log/info "Starting server on port " port)
    (run-server handler {:port port})
    (log/info "... started")))
