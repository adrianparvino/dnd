(ns dnd.battle
  (:require [dnd.phoenix-wrapper :refer [join push]]
            [re-frame.core :as rf]
            [re-frame.db :refer [app-db]]
            ["react-bootstrap" :as bs4])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]
                   [reagent.core :refer [with-let]]))

(rf/reg-event-fx
 ::initialize
 (fn [{:keys [db]} _]
   {:db (assoc db ::editing false ::characters [])
    :fx [[::join nil]]}))

(rf/reg-event-fx
 ::next
 (fn [{:keys [db]} [_ local]]
   {:db (update db ::characters (fn [[c & cs]] `(~@cs ~c)))
    :fx [(when local [::broadcast-next nil])]}))

(rf/reg-event-db
 ::set-characters
 (fn [db [_ characters]]
   (assoc db ::characters characters)))

(rf/reg-event-fx
 ::publish
 (fn [{:keys [db]} [_]]
   {:db (assoc db ::editing false)
    :fx [[::broadcast-set-characters]]}))

(rf/reg-event-db
 ::edit
 (fn [db [_ from to]]
   (update db ::characters (fn [characters] (for [c characters] (if (= c from) to c))))))

(rf/reg-event-db
 ::enable-editing
 (fn [db _]
   (assoc db ::editing true)))

(rf/reg-fx
 ::broadcast-next
 (fn [_]
   (push (::channel @app-db) :next)))

(rf/reg-fx
 ::broadcast-set-characters
 (fn [_]
   (let [{channel ::channel characters ::characters} @app-db]
       (push channel :set-characters characters))))

(rf/reg-fx
 ::join
 (fn [_]
   (swap! app-db (fn [{:keys [socket] :as db}]
                   (assoc db ::channel (join socket "battle"
                                          {:next #(rf/dispatch [::next false])
                                           :set-characters #(rf/dispatch [::set-characters (:cs %)])}
                                          #(rf/dispatch [::set-characters (:characters %)])))))))

(rf/reg-sub
 ::characters
 (fn [db _]
   (::characters db)))

(rf/reg-sub
 ::editing
 (fn [db _]
   (::editing db)))

;; -------------------------
;; Views

(defn character-item [props editing character]
  (if editing
    [:> bs4/ListGroup.Item props
     (letfn [(edit-name [event] 1)]
       [:> bs4/Form.Control {:class [:subtle-input] :style {:height "1.5em"} :placeholder character :on-blur #(rf/dispatch [::edit character (.. % -target -value)])}])]
    [:> bs4/ListGroup.Item props character]))

(defn character-turns [{:keys [class style] :as props}]
  (let [is-dm @(rf/subscribe [:is-dm])
        editing @(rf/subscribe [::editing])
        characters @(rf/subscribe [::characters])]
    [:> bs4/Col (merge props {:class (concat class [:d-flex :flex-column])})
     [:> bs4/ListGroup {:variant "flush" :class [:overflow-auto]}
      (for [c characters]
        [character-item {:key c :active (= c (first characters))} editing c])]
     (when is-dm
       [:<>
        (if editing
          [:> bs4/Button {:variant "secondary" :class [:mt-auto] :onClick #(rf/dispatch [::publish])} "Publish"]
          [:> bs4/Button {:variant "secondary" :class [:mt-auto] :onClick #(rf/dispatch [::enable-editing])} "Edit Initiative"])
        [:> bs4/Button {:onClick #(rf/dispatch [::next true])} "Next Turn"]])]))

(defn battle []
  [character-turns {:class [:px-0 :pr-md-3 :h-100] :md 4 :lg 3}])
