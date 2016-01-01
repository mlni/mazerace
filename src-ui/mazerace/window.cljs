(ns mazerace.window
  (:require [mazerace.log :as log]))

(defn on-resize-window []
  (let [h (aget js/window "innerHeight")
        w (aget js/window "innerWidth")
        size (max 100
                  (- (min w h) 50))
        container (aget (.getElementsByClassName js/document "game-container") 0)]
    (when container
      (log/info (str "on-resize " h " " w " " container size))
      (set! (.-width (.-style container)) (str size "px")))))

(defn handle-resize! []
  (let [timeout (atom nil)]
    (.addEventListener js/window "resize"
                       (fn [_]
                         (when @timeout (js/clearTimeout @timeout))
                         (reset! timeout (js/setTimeout on-resize-window 100))))))
