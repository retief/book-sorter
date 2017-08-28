(ns book-sorter.sente-api
  (:require [book-sorter.core :refer [book-data clean-books find-book]]
            [taoensso.sente :as sente]
            [taoensso.sente.server-adapters.aleph :as sente-adapter]))

(let [{:keys [ch-recv send-fn connected-uids
              ajax-post-fn ajax-get-or-ws-handshake-fn]}
      (sente/make-channel-socket! (sente-adapter/get-sch-adapter) {})]

  (def ring-ajax-post                ajax-post-fn)
  (def ring-ajax-get-or-ws-handshake ajax-get-or-ws-handshake-fn)
  (def ch-chsk                       ch-recv) ; ChannelSocket's receive channel
  (def chsk-send!                    send-fn) ; ChannelSocket's send API fn
  (def connected-uids                connected-uids)) ; Watchable, read-only atom

(defmulti handle-message
  "The main sente message handler"
  {:arglists '([[id data]])}
  (fn [[id]] id))

(defmethod handle-message :default
  [[id]]
  (when (not (= (namespace id) "chsk"))
    (throw (Exception. (str "Message not handled: " id)))))

(defn event-msg-handler [{event :event
                          id :id
                          data :?data
                          reply-fn :?reply-fn
                          uid :uid
                          client-id :client-id
                          ring-req :ring-req
                          ch-recv :ch-recv}]
  (println "event:" id data)
  (let [res (handle-message event)]
    (when reply-fn
      (reply-fn res))))

(defmethod handle-message :book/all
  [_]
  (clean-books @book-data))

(defmethod handle-message :book/get
  [[_ book-id]]
  (find-book book-id))

(sente/start-server-chsk-router! ch-chsk event-msg-handler)

#_(defn event-msg-handler [{reply-fn :?reply-fn :as ev-msg}]
    (prn "ev-msg" (select-keys ev-msg
                               [:event :id :?data
                                :?reply-fn :uid :client-id]))
    (prn "keys" (keys ev-msg))
    (when reply-fn
      (reply-fn [:hello-again "foo"])))

