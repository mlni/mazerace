(ns mazerace.window
  (:require [mazerace.log :as log]))

(defn resize-window []
  (let [h (aget js/window "innerHeight")
        w (aget js/window "innerWidth")
        container (aget (.getElementsByClassName js/document "container") 0)]
    (log/info (str "on-resize " h " " w " " container (str (min w h) "px")))
    (set! (.-width (.-style container)) (str (min w h) "px"))))

(defn handle-resize! []
  (let [timeout (atom nil)]
    (.addEventListener js/window "resize"
                       (fn [_]
                         (when @timeout (js/clearTimeout @timeout))
                         (reset! timeout (js/setTimeout resize-window 100))))
    (js/setTimeout resize-window 100)                       ; run once on startup as well
    ))
