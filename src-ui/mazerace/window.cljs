(ns mazerace.window
  (:require [mazerace.log :as log]))

(defn on-resize-window []
  (when-let [wrapper (aget (.getElementsByClassName js/document "game-wrapper") 0)]
    (let [wh (aget js/window "innerHeight")
          ww (aget js/window "innerWidth")
          w (aget wrapper "offsetWidth")
          nav-heigth 50
          size (max 100 (- (min wh ww w) nav-heigth))
          container (aget (.getElementsByClassName js/document "game-container") 0)]
      (when container
        (log/info "on-resize-window" wh ww w "->" size)
        (set! (.-width (.-style container)) (str size "px"))
        (set! (.-height (.-style container)) (str size "px"))))))

(defn handle-resize! []
  (let [timeout (atom nil)]
    (.addEventListener js/window "resize"
                       (fn [_]
                         (when @timeout (js/clearTimeout @timeout))
                         (reset! timeout (js/setTimeout on-resize-window 100))))))
