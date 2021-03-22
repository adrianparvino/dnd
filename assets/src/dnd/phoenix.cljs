(ns dnd.phoenix
  (:require [dnd.phoenix-wrapper :as wrapper]
            [cljs.core.async :refer [promise-chan chan put!]])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(defn connect [path]
  (wrapper/connect path))

(defn join [^phoenix/Socket socket channel-name & event-names]
  (let [result (promise-chan)
        event-channels (for [event-name event-names]
                         (let [ret (chan)]
                           [event-name #(put! ret %) ret]))
        event-cbs (for [[event-name cb _] event-channels]
                    [event-name cb])
        chans (for [[_ _ chan] event-channels] chan)
        handle (wrapper/join socket channel-name
                             event-cbs
                             #(put! result ["ok" %])
                             #(put! result ["error" %])
                             #(put! result ["timeout" %]))]
    `[~handle ~result ~@chans]))

(defn push [^phoenix/Channel handle event payload timeout]
  (let [result (promise-chan)]
    (wrapper/push handle event payload timeout
                  #(put! result ["ok" %])
                  #(put! result ["error" %])
                  #(put! result ["timeout" %]))
    result))
