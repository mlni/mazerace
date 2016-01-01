(ns mazerace.ui.pages
  (:require [reagent.core :as r]
            [mazerace.game :as game]
            [mazerace.ui.svg :as svg]
            [mazerace.socket :as ws]
            [mazerace.window :as window]
            [mazerace.log :as log]))

(defn index-page []
  [:div
   [:h1 "Mazerace"]
   [:button {:on-click #(game/start-game!)}
    "Play!"]])

(defn- overlay [content]
  [:div
   [:div.overlay-background]
   [:div.overlay-message
    content]])

(defn result [result]
  (overlay
    [:div
     (condp = result
       "win" "You win!"
       "lose" "Opponent won!"
       "opponent-disconnected" "Opponent disconnected"
       "Game over!")
     [:div
      [:button {:on-click #(game/start-game!)} "Play again!"]]]))

(defn connection-lost []
  (overlay [:div "Opponent disconnected"]))

(defn play []
  (let [game (game/game-state)
        connection-state (ws/state)]
    [:div.game-wrapper
     [:div.game-container
      (if (:result game)
        [result (:result game)]
        (if (not (:connected connection-state))
          [connection-lost]))
      (when (:maze game)
        [svg/render-maze game])]]))

(defn play-component [game]
  (r/create-class
    {:component-did-mount window/on-resize-window
     :reagent-render      play}))

(defn connecting []
  ; TODO: handle "connecting" state
  [:h1 "Waiting for an opponent"])

(defn navbar []
  [:nav {:class "navbar navbar-default navbar-fixed-top"}
   [:div.container
    [:div.navbar-header
     [:a.navbar-brand "Mazerace"]]]])

(defn content []
  (let [game (game/game-state)]
    [:div
     [navbar]
     [:div.container
      (condp = (:state game) :connecting [connecting]
                             :playing [play-component game]
                             [index-page])]]))
