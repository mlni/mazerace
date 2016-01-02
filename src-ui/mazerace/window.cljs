(ns mazerace.window)

(defn on-resize-window []
  (when-let [wrapper (aget (.getElementsByClassName js/document "game-wrapper") 0)]
    (let [h (aget js/window "innerHeight")
          w (aget wrapper "offsetWidth")
          size (max 100 (min w h))
          container (aget (.getElementsByClassName js/document "game-container") 0)]
      (when container
        (set! (.-width (.-style container)) (str size "px"))))))

(defn handle-resize! []
  (let [timeout (atom nil)]
    (.addEventListener js/window "resize"
                       (fn [_]
                         (when @timeout (js/clearTimeout @timeout))
                         (reset! timeout (js/setTimeout on-resize-window 100))))))
