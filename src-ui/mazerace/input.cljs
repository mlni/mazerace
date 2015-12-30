(ns mazerace.input)

(defn parse-direction [e]
  (condp = (.-keyCode e) 38 :up
                         87 :up                             ; w
                         75 :up                             ; j
                         40 :down
                         83 :down                           ; s
                         74 :down                           ; k
                         37 :left
                         65 :left                           ; a
                         72 :left                           ; h
                         39 :right
                         68 :right                          ; d
                         76 :right                          ; l
                         :none))

(defn register-handler [handler-fn]
  (set! (.-onkeydown js/document)
        (fn [e]
          (let [dir (parse-direction e)
                modifiers (or (.-altKey e) (.-ctrlKey e) (.-metaKey e) (.-shiftKey e))]
            (when (and (not modifiers) (not= dir :none))
              (.preventDefault e)
              (handler-fn dir))))))