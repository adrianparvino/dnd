(ns dnd.core
  (:require [dnd.phoenix :refer [connect join push]]
            [dnd.state :refer [socket]]
            [cljs.core.async :refer [<! alt! alts! chan put!]]
            [reagent.core :as r]
            [reagent.dom :as rdom]
            ["react-bootstrap" :as bs4])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]
                   [reagent.core :refer [with-let]]))

(defonce dnd-map
  (r/atom
   [['grass 'grass 'grass 'grass 'grass]
    ['grass 'water 'water 'grass 'grass]
    ['grass 'water 'grass 'grass 'grass]
    ['grass 'water 'grass 'water 'grass]
    ['grass 'grass 'grass 'grass 'grass]]))

(defonce dnd-map-foreground
  (r/atom
   [['none 'none 'none   'none 'none]
    ['none 'none 'none   'none 'none]
    ['none 'none 'leaves 'none 'none]
    ['none 'none 'trunk  'none 'none]
    ['none 'none 'none   'none 'none]]))

(defonce characters (r/atom []))
(defonce next-button-chan (chan))
(defn next-turn [] (put! next-button-chan []))

;; -------------------------
;; Views

(defn character-turns [{:keys [class style] :as props}]
  (let [[c & cs] @characters]
    [:> bs4/Col (merge props {:class (concat class [:d-flex :flex-column])})
     [:> bs4/ListGroup {:variant "flush" :class [:overflow-auto]}
      [:> bs4/ListGroup.Item {:active true} c]
      (for [character cs]
        [:> bs4/ListGroup.Item character])]
     [:> bs4/ListGroup.Item {:class [:border-0 :mt-auto] :action true :onClick next-turn} "Next Turn"]]))

(defn battle []
  [:<>
   [character-turns {:class [:pl-0 :h-100] :md 4} ]
   [:> bs4/Col {:md 8}
    [:div {:class ["map-grid"] :style {:width "80px" :height "80px"}}
     (for [cell (apply concat @dnd-map)]
       [:div {:class [cell]}])]
    [:div {:class ["map-grid"] :style {:width "80px" :height "80px"}}
     (for [cell (apply concat @dnd-map-foreground)]
       [:div {:class [cell]}])]]])

;; -------------------------
;; Initialize app

(defn mount []
  (rdom/render [battle] (js/document.getElementById "react-app")))

(defn ^:export init []
  (reset! socket (connect "/socket"))
  (let [[handle result next-chan] (join @socket "battle" "next")]
    (go (let [[status {cs :characters}] (<! result)]
          (case status
            "ok" (reset! characters cs)))
        (loop []
          (alt!
            next-chan ([result] nil)
            next-button-chan ([result] (push handle "next" #js {} 0)))
          (swap! characters (fn [[c & cs]] `(~@cs ~c)))
          (recur))))
  (mount))
