(ns dnd.battle
  (:require [dnd.phoenix :refer [join push]]
            [dnd.state :refer [socket is-dm]]
            [cljs.core.async :refer [<! alt! alts! chan put!]]
            [reagent.core :as r]
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

(defonce editing (r/atom false))
(defonce edit-button-chan (chan))
(defonce edit-item-chan (chan))
(defn toggle-edit [] (put! edit-button-chan []))

;; -------------------------
;; Views

(defn character-item [{:as props :or {}} & [character]]
  (if @editing
    [:> bs4/ListGroup.Item props
     (letfn [(edit-name [event]
               (.preventDefault event)
               (put! edit-item-chan [character (-> event
                                                   .-target
                                                   .-elements
                                                   (aget "new-name")
                                                   .-value)]))]
       [:> bs4/Form {:onSubmit edit-name}
        [:> bs4/Form.Control {:class [:rounded-0 :border-top-0 :border-left-0 :border-right-0 :p-0 :bg-transparent] :style {:height "24px"} :name "new-name" :placeholder character}]])]
    [:> bs4/ListGroup.Item props character]))

(defn character-turns [{:keys [class style] :as props}]
  (let [[c & cs] @characters]
    [:> bs4/Col (merge props {:class (concat class [:d-flex :flex-column])})
     [:> bs4/ListGroup {:variant "flush" :class [:overflow-auto]}
      [character-item {:active true} c]
      (for [c cs]
        [character-item {} c])]
     (when @is-dm
       (list
        [:> bs4/Button {:class [:mt-auto] :onClick toggle-edit}
         (if @editing
           "Publish"
           "Edit Initiative")]
        [:> bs4/Button {:onClick next-turn} "Next Turn"]))]))

(defn battle []
  (with-let [[handle result next-chan set-characters-chan] (join @socket "battle" "next" "set-characters")
             _ (go (let [[status {cs :characters}] (<! result)]
                     (case status
                       "ok" (reset! characters cs)))
                   (loop []
                     (alt!
                       next-chan ([_] (swap! characters (fn [[c & cs]] `(~@cs ~c))))
                       next-button-chan ([_]
                                         (push handle "next" #js {} 0)
                                         (swap! characters (fn [[c & cs]] `(~@cs ~c))))
                       set-characters-chan ([{:keys [cs]}]
                                            (reset! characters cs))
                       edit-button-chan ([_]
                                         (reset! editing true)
                                         (loop []
                                           (alt!
                                             next-chan ([_] (recur))
                                             next-button-chan ([_] (recur))
                                             edit-item-chan ([[from to]]
                                                             (when-not (some #(= to %) @characters)
                                                               (swap! characters
                                                                      (fn [characters]
                                                                        (for [c characters]
                                                                          (if (= c from) to c)))))
                                                             (recur))
                                             edit-button-chan ([result]
                                                               (push handle "set-characters" (js->clj @characters) 0))))
                                         (reset! editing false)))
                     (recur)))]
    [:<>
     [character-turns {:class [:pl-0 :h-100] :md 4 :lg 3}]
     [:> bs4/Col {:md 8 :lg 9}
      [:div {:class ["map-grid"] :style {:width "80px" :height "80px"}}
       (for [cell (apply concat @dnd-map)]
         [:div {:class [cell]}])]
      [:div {:class ["map-grid"] :style {:width "80px" :height "80px"}}
       (for [cell (apply concat @dnd-map-foreground)]
         [:div {:class [cell]}])]]]))
