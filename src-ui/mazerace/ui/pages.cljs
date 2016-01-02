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
   [:button {:on-click #(game/dispatch :start-game)}
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
      [:button {:on-click #(game/dispatch :start-game)} "Play again!"]]]))

(defn connection-lost []
  (overlay
    [:div
     "Opponent disconnected"
     [:div
      [:button {:on-click #(game/dispatch :start-game)} "Play again!"]]]))

(defn play []
  (let [game (game/game-state)
        connection-state (ws/state)]
    [:div.game-wrapper
     [:div.game-container
      (if (:result game)
        [result (:result game)]
        (when-not (:connected connection-state)
          [connection-lost]))
      (when (:maze game)
        [svg/render-maze game])]]))

(defn play-component []
  (r/create-class
    {:component-did-mount window/on-resize-window
     :reagent-render      play}))

(defn connecting []
  (let [connection-state (ws/state)]
    (if (:connecting connection-state)
      [:h1 "Connecting ..."]
      (if (not (:connected connection-state))
        [:h1 "Err, connection lost. Bummer."]
        [:div
         [:h1 "Waiting for an opponent"]
         [:button {:on-click #(game/dispatch :play-against-computer)}
          "Play against computer"]]))))

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
                             :playing [play-component]
                             [index-page])]]))
