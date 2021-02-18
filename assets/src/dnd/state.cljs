(ns dnd.state
  (:require [reagent.core :as r]))

(defonce socket (r/atom nil))
(defonce is-dm (r/atom false))

(defn set-dm []
  (reset! is-dm false))

(defn reset-dm []
  (reset! is-dm true))

(defn toggle-dm []
  (swap! is-dm not))
