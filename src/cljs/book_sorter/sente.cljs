(ns book-sorter.sente
  (:require [taoensso.sente :as s]
            [re-frame.core :as rf]))

(enable-console-print!)

(defn- dummy [& args]
  nil)

(defonce chsk-map
  (try
    (s/make-channel-socket! "/chsk"
                            {:type :auto})
    (catch js/Object e
      {:chsk nil
       :ch-recv nil
       :send-fn dummy
       :state nil})))

(let [{:keys [chsk ch-recv send-fn state]} chsk-map]
  (defonce chsk       chsk)
  (defonce ch-chsk    ch-recv)       ; ChannelSocket's receive channel
  (defonce chsk-send! send-fn)       ; ChannelSocket's send API fn
  (defonce chsk-state state))        ; Watchable, read-only atom

(def pending-messages (atom []))

(defn deref-reset!
  [atm new-value]
  (let [old-value @atm]
    (if (compare-and-set! atm old-value new-value)
      old-value
      (recur atm new-value))))

(defn raw-send-message! [& msg]
  (when (not (apply chsk-send! msg))
    (do (println "delaying message:")
        (prn msg)
        (swap! pending-messages conj msg))))

(add-watch chsk-state ::channel-state-watcher
           (fn [key atom old new]
             (when (:open? new)
               (let [msgs (deref-reset! pending-messages [])]
                 (println "sending messages:")
                 (prn msgs)
                 (doseq [msg msgs]
                   (apply chsk-send! msg))))))

(defn send-message! [{:keys [event on-success on-failure timeout]}]
  (let [cb (when (or on-success on-failure)
             (fn [cb-reply]
               (prn "sente's reply:" cb-reply)
               (if (s/cb-success? cb-reply)
                 (when on-success
                   (rf/dispatch (conj on-success cb-reply)))
                 (when on-failure
                   (rf/dispatch (conj on-failure cb-reply))))))
        timeout (and cb (or timeout 2000))]
    (println "sending sente")
    (prn event on-success on-failure timeout)
    (raw-send-message! event timeout cb)
    (println "sente sent")))

(defmulti handle-message
  "The main client sente message handler"
  {:arglists '([[id data]])}
  (fn [[id]] id))

(defmethod handle-message :default
  [[id :as event]]
  (when (not (= (namespace id) "chsk"))
    (prn "Message not handled" event)))

(defmethod handle-message :chsk/recv
  [[_ server-event]]
  (rf/dispatch server-event))

(defn event-msg-handler [{:keys [event]}]
  (prn "Message received" event)
  (handle-message event))

(s/start-client-chsk-router! ch-chsk event-msg-handler)

(rf/reg-fx
  :sente/send
  send-message!)
