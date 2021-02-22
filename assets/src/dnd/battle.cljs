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

(defonce handle (r/atom nil))

;; -------------------------
;; Views

(defn character-item [props editing character]
  (if editing
    [:> bs4/ListGroup.Item props
     (letfn [(edit-name [event] (put! edit-item-chan [character (.. event -target -value)]))]
       [:> bs4/Form.Control {:class [:subtle-input] :style {:height "1.5em"} :placeholder character :on-blur edit-name}])]
    [:> bs4/ListGroup.Item props character]))

(defn character-turns [{:keys [class style] :as props}]
  (with-let [_ (go-loop []
                 (<! edit-button-chan)
                 (reset! editing true)
                 (loop []
                   (alt!
                     edit-item-chan ([[from to]]
                                     (when-not (some #(= to %) @characters)
                                       (swap! characters
                                              (fn [characters]
                                                (for [c characters]
                                                  (if (= c from) to c)))))
                                     (recur))
                     edit-button-chan ([result]
                                       (push @handle "set-characters" (js->clj @characters) 0))))
                 (reset! editing false)
                 (recur))])
  [:> bs4/Col (merge props {:class (concat class [:d-flex :flex-column])})
   [:> bs4/ListGroup {:variant "flush" :class [:overflow-auto]}
    (doall
     (for [c @characters]
       [character-item {:key c :active (= c (first @characters))} @editing c]))]
   (when @is-dm
     [:<>
      [:> bs4/Button {:variant "secondary" :class [:mt-auto] :onClick toggle-edit}
       (if @editing
         "Publish"
         "Edit Initiative")]
      [:> bs4/Button {:onClick next-turn} "Next Turn"]])])

(defn battle []
  (with-let [[h result next-chan set-characters-chan] (join @socket "battle" "next" "set-characters")
             _ (go (reset! handle h)
                   (let [[status {cs :characters}] (<! result)]
                     (case status
                       "ok" (reset! characters cs)))
                   (loop []
                     (alt!
                       next-chan ([_] (swap! characters (fn [[c & cs]] `(~@cs ~c))))
                       next-button-chan ([_]
                                         (push @handle "next" #js {} 0)
                                         (swap! characters (fn [[c & cs]] `(~@cs ~c))))
                       set-characters-chan ([{:keys [cs]}]
                                            (reset! characters cs))
)
                     (recur)))]
    [:<>
     [character-turns {:class [:px-0 :pr-md-3 :h-100] :md 4 :lg 3}]
     [:> bs4/Col {:md 8 :lg 9}
      [:div {:class ["map-grid"] :style {:width "80px" :height "80px"}}
       (for [cell (apply concat @dnd-map)]
         [:div {:class [cell]}])]
      [:div {:class ["map-grid"] :style {:width "80px" :height "80px"}}
       (for [cell (apply concat @dnd-map-foreground)]
         [:div {:class [cell]}])]]]))
