(ns mazerace.log
  (:require [clojure.string :as string]))

(defn info [& args]
  (let [msg (string/join " " (map str args))]
    (js/console.log (.getTime (js/Date.)) msg)))