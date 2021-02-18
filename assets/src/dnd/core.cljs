(ns dnd.core
  (:require [dnd.phoenix :refer [connect]]
            [dnd.battle :refer [battle]]
            [dnd.state :refer [socket]]
            [reagent.dom :as rdom]
            ["react-bootstrap" :as bs4])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]
                   [reagent.core :refer [with-let]]))

;; -------------------------
;; Views

(defn root []
  [:<>
   [:> bs4/Navbar
    [:> bs4/Navbar.Brand "Forest and Felines"]
    [:> bs4/Navbar.Collapse {:class [:justify-content-end]}
     "DM Mode"]]
   [:main {:role "main" :class [:container-fluid :flex-grow-1 :overflow-hidden]}
    [:> bs4/Row {:class [:h-100]}
     [battle]]]])

;; -------------------------
;; Initialize app

(defn mount []
  (rdom/render [root] (js/document.getElementById "react-app")))

(defn ^:export init []
  (reset! socket (connect "/socket"))
  (mount))
