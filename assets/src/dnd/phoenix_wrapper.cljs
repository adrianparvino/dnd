(ns dnd.phoenix-wrapper
  (:require ["phoenix" :as phoenix]))

(defn connect [path]
  (let [socket (phoenix/Socket. path)]
    (.connect socket)
    socket))

(defn join
  ([^phoenix/Socket socket channel-name handlers]
   (join socket channel-name handlers (fn [_] nil) (fn [_] nil) (fn [_] nil)))
  ([^phoenix/Socket socket channel-name handlers ok-cb]
   (join socket channel-name handlers ok-cb (fn [_] nil) (fn [_] nil)))
  ([^phoenix/Socket socket channel-name handlers ok-cb error-cb]
   (join socket channel-name handlers ok-cb error-cb (fn [_] nil)))
  ([^phoenix/Socket socket channel-name handlers ok-cb error-cb timeout-cb]
   (let [^phoenix/Channel handle (.channel socket channel-name)]
     (doseq [[event cb] handlers] (do (.on handle (name event) #(cb (js->clj % :keywordize-keys true)))))
     (-> handle
         ^phoenix/Channel (.join)
         ^phoenix/Channel (.receive "ok"      (fn [reply] (ok-cb (js->clj reply :keywordize-keys true))))
         ^phoenix/Channel (.receive "error"   (fn [reply] (error-cb (js->clj reply :keywordize-keys true))))
         ^phoenix/Channel (.receive "timeoutmode" (fn [reply] (timeout-cb (js->clj reply :keywordize-keys true)))))
     handle)))

(defn push
  ([^phoenix/Channel handle event]
   (push handle event {} 0 (fn [_] nil) (fn [_] nil) (fn [_] nil)))
  ([^phoenix/Channel handle event payload]
   (push handle event payload 0 (fn [_] nil) (fn [_] nil) (fn [_] nil)))
  ([^phoenix/Channel handle event payload timeout]
   (push handle event payload timeout (fn [_] nil) (fn [_] nil) (fn [_] nil)))
  ([^phoenix/Channel handle event payload timeout ok-cb]
   (push handle event payload timeout ok-cb (fn [_] nil) (fn [_] nil)))
  ([^phoenix/Channel handle event payload timeout ok-cb error-cb]
   (push handle event payload timeout ok-cb error-cb (fn [_] nil)))
  ([^phoenix/Channel handle event payload timeout ok-cb error-cb timeout-cb]
   (-> handle
       ^phoenix/Channel (.push (name event) (clj->js payload) timeout)
       ^phoenix/Channel (.receive "ok"      (fn [reply] (ok-cb (js->clj reply :keywordize-keys true))))
       ^phoenix/Channel (.receive "error"   (fn [reply] (error-cb (js->clj reply :keywordize-keys true))))
       ^phoenix/Channel (.receive "timeout" (fn [reply] (timeout-cb (js->clj reply :keywordize-keys true)))))
   handle))
