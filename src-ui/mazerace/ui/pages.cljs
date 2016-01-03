(ns mazerace.ui.pages
  (:require [reagent.core :as r]
            [mazerace.game :as game]
            [mazerace.ui.svg :as svg]
            [mazerace.socket :as ws]
            [mazerace.window :as window]))

(defn- start-button []
  [:button.btn.btn-default {:on-click #(game/dispatch :start-game)}
   [:img {:src "/icons/play.svg"}]
   "Start game!"])

(defn index-page []
  [:div.masthead
   [:img.sample {:src "/imgs/game.png"}]
   [:div.slogan
    "Help the mouse get to the cheese before the other player does"]
   [start-button]])

(defn- overlay [content]
  [:div
   [:div.overlay-background]
   [:div.overlay-message
    content]])

(defn game-result [result]
  (overlay
    [:div
     (condp = result
       "win" "You win!"
       "lose" "Opponent won!"
       "opponent-disconnected" "Opponent disconnected"
       "Game over!")
     [:div
      [start-button]]]))

(defn connection-lost []
  (overlay
    [:div
     "Opponent disconnected"
     [:div [start-button]]]))

(defn play []
  (let [game (game/game-state)
        connection-state (ws/state)]
    [:div.game-wrapper
     [:div.game-container
      (if (:result game)
        [game-result (:result game)]
        (when-not (:connected connection-state)
          [connection-lost]))
      (when (:maze game)
        [svg/render-game])]]))

(defn play-component []
  (r/create-class
    {:component-did-mount window/on-resize-window
     :reagent-render      play}))

(defn connecting []
  (let [connection-state (ws/state)]
    (if (:connecting connection-state)
      [:h1 "Connecting ..."]
      (if (not (:connected connection-state))
        [:h1 "Connection lost. Bummer."]
        [:div.connecting
         [:h1 "Waiting for an opponent ..."]
         [:div "You can wait a bit for another player to play against or you can play against the computer. I don't mind either way."]
         [:button.btn.btn-default {:on-click #(game/dispatch :play-against-computer)}
          "Play against computer"]]))))

(defn navbar []
  [:nav {:class "navbar navbar-default navbar-fixed-top"}
   [:div.container
    [:div.navbar-header
     [:a.navbar-brand "Mazerace"]]
    [:div.nav
     [:ul.nav.navbar-nav
      [:li [:a {:href "#"} "Home"]]
      [:li [:a {:href "#"} "About"]]]]]])

(defn content []
  (let [game (game/game-state)]
    [:div
     [navbar]
     [:div.container
      (condp = (:state game) :connecting [connecting]
                             :playing [play-component]
                             [index-page])]]))
