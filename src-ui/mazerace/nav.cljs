(ns mazerace.nav
  (:require [secretary.core :as s]
            [goog.events :as events]
            [mazerace.game :as g])
  (:require-macros [secretary.core :refer [defroute]])
  (:import goog.History
           goog.history.EventType))

(s/set-config! :prefix "#")

(defroute "/" []
          (g/dispatch :navigate :home))
(defroute "/about" []
          (g/dispatch :navigate :about))
(defroute "/play" []
          (g/dispatch :navigate :connecting))
(defroute "*" []
          (g/dispatch :navigate :home))

(defonce history (doto (History.)
                   (events/listen EventType.NAVIGATE #(s/dispatch! (.-token %)))
                   (.setEnabled true)))

(defn nav! [path]
  (.setToken history path))