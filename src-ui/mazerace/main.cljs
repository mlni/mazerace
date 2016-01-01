(ns mazerace.main
  (:require [reagent.core :as r]
            [mazerace.log :as log]
            [mazerace.ui.pages :as pages]
            [mazerace.window :as window]))

(log/info "Starting ...")

(r/render [pages/content]
          (.getElementById js/document "app"))

(window/handle-resize!)

(log/info "Started")
