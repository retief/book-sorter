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

(defn send-message! [& msg]
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

(defn handle-sente-send [{event :event
                          on-success :on-success
                          on-failure :on-failure
                          timeout :timeout}]
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
    (send-message! event timeout cb)
    (println "sente sent")))

(rf/reg-fx
  :sente/send
  handle-sente-send)
