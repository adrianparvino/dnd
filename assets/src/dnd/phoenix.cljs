(ns dnd.phoenix
  (:require ["phoenix" :as phoenix]
            [cljs.core.async :refer [chan promise-chan put!]])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(defn connect [path]
  (let [socket (phoenix/Socket. path)]
    (.connect socket)
    socket))

(defn join [^phoenix/Socket socket channel-name & events]
  (let [^phoenix/Channel handle (.channel socket channel-name)
        result (promise-chan)
        event-channels (for [event events]
                         (let [ret (chan)]
                           (.on handle event #(put! ret %))
                           ret))]
    (-> handle
        ^phoenix/Channel (.join)
        ^phoenix/Channel (.receive "ok"      (fn [reply] (put! result ["ok"      (js->clj reply :keywordize-keys true)])))
        ^phoenix/Channel (.receive "error"   (fn [reply] (put! result ["error"   (js->clj reply :keywordize-keys true)])))
        ^phoenix/Channel (.receive "timeout" (fn [reply] (put! result ["timeout" (js->clj reply :keywordize-keys true)]))))
    `[~handle ~result ~@event-channels]))

(defn push [^phoenix/Channel handle event payload timeout]
  (let [result (promise-chan)]
    (-> handle
        ^phoenix/Channel (.push event (clj->js payload) timeout)
        ^phoenix/Channel (.receive "ok"      (fn [reply] (put! result ["ok"      (js->clj reply :keywordize-keys true)])))
        ^phoenix/Channel (.receive "error"   (fn [reply] (put! result ["error"   (js->clj reply :keywordize-keys true)])))
        ^phoenix/Channel (.receive "timeout" (fn [reply] (put! result ["timeout" (js->clj reply :keywordize-keys true)]))))
    result))
