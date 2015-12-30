(ns mazerace.log)

(defn info [msg]
  (js/console.log (.getTime (js/Date.)) msg))