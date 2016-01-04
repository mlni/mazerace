(ns mazerace.ui.pages
  (:require [reagent.core :as r]
            [mazerace.game :as game]
            [mazerace.nav :as n]
            [mazerace.ui.svg :as svg]
            [mazerace.socket :as ws]
            [mazerace.window :as window]
            [mazerace.game :as g]))


(defn index-page []
  [:div.masthead
   [:img.sample {:src "/imgs/game.png"}]
   [:div.slogan
    "Help your mouse get to the cheese before the other player does"]
   [:button.btn.btn-success {:on-click #(n/nav! "/play")}
    [:img {:src "/icons/play.svg"}]
    "Start game!"]])

(defn- overlay [content]
  [:div
   [:div.overlay-background]
   [:div.overlay-message
    content]])

(defn- restart-button []
  [:button.btn.btn-success {:on-click #(g/dispatch :start-game)}
   [:img {:src "/icons/play.svg"}]
   "Start game!"])

(defn game-result [result]
  (overlay
    [:div
     (condp = result
       "win" "You won!"
       "lose" "Opponent won!"
       "opponent-disconnected" "Opponent disconnected"
       "Game over!")
     [:div
      [restart-button]]]))

(defn connection-lost []
  (overlay
    [:div
     "Opponent disconnected"
     [:div [restart-button]]]))

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

(defn about []
  [:h1 "About"])

(defn navbar []
  [:nav {:class "navbar navbar-default navbar-fixed-top"}
   [:div.container
    [:div.navbar-header
     [:a.navbar-brand {:href "#/"} "Mazerace"]]]])

(defn content []
  (let [game (game/game-state)]
    [:div
     [navbar]
     [:div.container
      (condp = (:state game) :connecting [connecting]
                             :playing [play-component]
                             :about [about]
                             [index-page])]]))
