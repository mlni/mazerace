(ns mazerace.core
  (:use org.httpkit.server
        (compojure [core :only [defroutes GET POST]]
                   [route :only [files not-found resources]]
                   [handler :only [site]]))
  (:require [ring.middleware.reload :as reload]
            [ring.util.response :as resp]
            [mazerace.connectivity :as conn]
            [mazerace.socket :as ws]
            [clojure.core.async :refer [close! <! >! chan go go-loop]]
            [clojure.tools.logging :as log])
  (:gen-class))

; TODO: move into socket.clj,


(defroutes app
           (GET "/" [] (resp/redirect "index.html"))
           (GET "/ws" [] ws/socket-handler)
           (resources "/" {:root "/public"})
           (not-found "Page not found"))

(defn -main [& args]
  (let [handler (-> #'app site reload/wrap-reload ring.middleware.keyword-params/wrap-keyword-params ring.middleware.params/wrap-params)
        port (Integer/parseInt (or (System/getenv "PORT") "3000"))]
    (log/info "Starting server on port " port)
    (run-server handler {:port port})
    (log/info "Starting connection handler")
    (conn/start-connection-handler)
    (log/info "... done")))
