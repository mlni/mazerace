(ns mazerace.ui.pages
  (:require [mazerace.game :as game]
            [mazerace.ui.svg :as svg]
            [mazerace.socket :as ws]))

(defn index []
  (let [game (game/game-state)
        connection-state (ws/state)]
    [:div.container
     [:h1 (if (:result game)
            (:result game)
            (if (:maze game)
              "Race!"
              "Waiting for an opponent..."))]
     [:h5 (if (:connecting connection-state)
            "Connecting..."
            (if (not (:connected connection-state))
              "Disconnected"))]
     (when (and (:connected connection-state) (:maze game))
       [svg/render-maze])]))