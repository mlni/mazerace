(ns mazerace.ui.pages
  (:require [mazerace.game :as game]
            [mazerace.ui.svg :as svg]
            [mazerace.socket :as ws]))

(defn index-page []
  [:div
   [:h1 "Mazerace"]
   [:button {:on-click #(game/start-game!)}
    "Play!"]])

(defn- overlay [& content]
  [:div
   [:div.overlay-background]
   [:div.overlay-message
    content]])

(defn result [result]
  (overlay
    [:div (condp = result
            "win" "You win!"
            "lose" "Opponent won!"
            "opponent-disconnected" "Opponent disconnected"
            "Game over!")]
    [:div [:button {:on-click #(game/start-game!)} "Play again!"]]))

(defn connection-lost []
  (overlay "Opponent disconnected"))

(defn play [game]
  (let [connection-state (ws/state)]
    [:div.game
     (if (:result game)
       [result (:result game)]
       (if (not (:connected connection-state))
         [connection-lost]))
     (when (:maze game)
       [svg/render-maze game])]))

(defn connecting []
  ; TODO: handle "connecting" state
  [:h1 "Waiting for an opponent"])

(defn content []
  (let [game (game/game-state)]
    [:div.container
     (condp = (:state game) :connecting [connecting]
                            :playing [play game]
                            [index-page])]))
