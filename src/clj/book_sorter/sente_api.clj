(ns book-sorter.sente-api
  (:require [book-sorter.core :as core]
            [taoensso.sente :as sente]
            [taoensso.sente.server-adapters.aleph :as sente-adapter]
            [book-sorter.subscription-manager :as sm]))

(let [{:keys [ch-recv send-fn connected-uids
              ajax-post-fn ajax-get-or-ws-handshake-fn]}
      (sente/make-channel-socket! (sente-adapter/get-sch-adapter)
                                  {:user-id-fn (fn [ring-req]
                                                 (:client-id ring-req))})]

  (def ring-ajax-post                ajax-post-fn)
  (def ring-ajax-get-or-ws-handshake ajax-get-or-ws-handshake-fn)
  (def ch-chsk                       ch-recv) ; ChannelSocket's receive channel
  (def chsk-send!                    send-fn) ; ChannelSocket's send API fn
  (def connected-uids                connected-uids)) ; Watchable, read-only atom

(defmulti handle-message
  "The main sente message handler"
  {:arglists '([[id data]])}
  (fn [[id] _] id))

(defmethod handle-message :default
  [[id] _]
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
  (println "event:" event id data)
  (let [res (handle-message event uid)]
    (when reply-fn
      (reply-fn res))))

(def subscriptions (atom sm/base-manager))

(defmethod handle-message :book/data
  [[id requests] uid]
  (prn id requests uid)
  (->> requests
       (map #(vector % (core/get-data %)))
       (into {})))

(sente/start-server-chsk-router! ch-chsk event-msg-handler)

#_(defn event-msg-handler [{reply-fn :?reply-fn :as ev-msg}]
    (prn "ev-msg" (select-keys ev-msg
                               [:event :id :?data
                                :?reply-fn :uid :client-id]))
    (prn "keys" (keys ev-msg))
    (when reply-fn
      (reply-fn [:hello-again "foo"])))

