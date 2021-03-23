(ns dnd.core
  (:require [phoenix.interop :refer [connect]]
            [dnd.battle :refer [battle]]
            [reagent.dom :as rdom]
            [re-frame.core :as rf]
            [re-frame.db :refer [app-db]]
            ["bootstrap-switch-button-react"
             :rename {default BootstrapSwitchButton}]
            ["react-bootstrap" :as bs4])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]
                   [reagent.core :refer [with-let]]))

(rf/reg-event-db
 :toggle-dm
 (fn [db _]
   (update db :is-dm not)))

(rf/reg-event-fx
 :initialize
 (fn [_ _]
   {:connect "/socket"
    :db {:is-dm false}}))

(rf/reg-fx
 :connect
 (fn [url]
   (swap! app-db #(assoc % :socket (connect url)))))

(rf/reg-sub
 :is-dm
 (fn [db query-vec]
   (:is-dm db)))

;; -------------------------
;; Views

(defn root []
  [:<>
   [:> bs4/Navbar
    [:> bs4/Navbar.Brand "Forest and Felines"]
    [:> bs4/Navbar.Collapse {:class [:justify-content-end]}
     [:> BootstrapSwitchButton
      {:offlabel "Player"
       :onlabel "DM"
       :checked @(rf/subscribe [:is-dm])
       :onChange #(rf/dispatch [:toggle-dm])
       :width 100}]]]
   [:main {:role "main" :class [:container-fluid :flex-grow-1 :overflow-hidden]}
    [:> bs4/Row {:class [:h-100]}
     [battle]]]])

;; -------------------------
;; Initialize app

(defn mount []
  (rdom/render [root] (js/document.getElementById "react-app")))

(defn ^:export init []
  (rf/dispatch [:initialize])
  (rf/dispatch [:dnd.battle/initialize])
  (mount))
