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
  "The main server sente message handler"
  {:arglists '([[id data] uid])}
  (fn [[id] _] id))

(defmethod handle-message :default
  [[id] _]
  (when (not (= (namespace id) "chsk"))
    (throw (Exception. (str "Message not handled: " id)))))

(defn event-msg-handler [{:keys [event id uid ?data ?reply-fn]}]
  (println "event:" event id ?data)
  (let [res (handle-message event uid)]
    (when ?reply-fn
      (?reply-fn res))))

(def subscriptions (atom sm/base-manager))

(defn update-sub! [sub]
  (let [data (core/get-data sub)
        uids ((:by-sub @subscriptions) sub)]
    (prn "all uids" @connected-uids)
    (prn "updating subs" @subscriptions sub uids)
    (doseq [uid uids]
      (prn "updating sub" uid [:client/update-subs {sub data}])
      (chsk-send! uid [:client/update-subs {sub data}]))))

(defmethod handle-message :book/data
  [[id requests] uid]
  (prn id requests uid)
  (swap! subscriptions sm/add-subs uid requests)
  (prn "add-sub:" @subscriptions)
  (->> requests
       (map #(vector % (core/get-data %)))
       (into {})))

(defmethod handle-message :book/clear-subs
  [[id subs] uid]
  (prn id subs uid)
  (swap! subscriptions sm/remove-subs uid subs)
  (prn "clear-sub:" @subscriptions))

(defmethod handle-message :book/set-tag
  [[id {tag :tag book-id :id}] uid]
  (prn id book-id uid)
  (core/set-tag! book-id tag)
  (update-sub! [:book/get book-id]))

(sente/start-server-chsk-router! ch-chsk event-msg-handler)

#_(defn event-msg-handler [{reply-fn :?reply-fn :as ev-msg}]
    (prn "ev-msg" (select-keys ev-msg
                               [:event :id :?data
                                :?reply-fn :uid :client-id]))
    (prn "keys" (keys ev-msg))
    (when reply-fn
      (reply-fn [:hello-again "foo"])))

