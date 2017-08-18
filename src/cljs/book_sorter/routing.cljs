(ns book-sorter.routing
  (:require [clojure.string :as str]
            [re-frame.core :as rf]
            [bidi.bidi :as b]
            [pushy.core :as p]
            [book-sorter.urls :as u]))

(enable-console-print!)
(defn log [& x]
  (apply prn x)
  (last x))

;; Client routing involved 2 libraries, 2 events, and a fx
;; Urls are always {:handler :foo, :params {:id bar}, :query {"a" "b"}}
;;
;; If want to go somewhere programmatically, we fire a :navigate event with a url
;; navigate just passes the url to the :routing/update-history fx
;; the :routing/update-history fx tells pushy to update the url
;; pushy then notices that the url updated and fires the :routing/page-changed event
;; :routing/page-changed hits the handle-page-changed multimethod, which
;;   dispatches based on the url handler
;; specializations of handle-page-changed do the actual processing to generate
;;   the new page
;;
;; To replace the url without adding a history frame, add a third truthy element
;; to the :navigate event vector
;;
;; When the user clicks a link, pushy notices the page change and control
;; continues as above
;;
;; this feels a bit overly complex, but pushy always triggers when you tell
;; it to update its history, so the "tell pushy to update" event has to be
;; separate from the "actually change the page" event that pushy fires
;; and we don't want to have side effects directly in event handlers as a
;; general principle

(defn split-url [url]
  (let [[base params] (str/split url #"\?" 2)
        base-url {:url base}]
    (if params
      (assoc base-url :query
             (into {}
                   (for [pair (str/split params #"&")]
                     (vec (map b/url-decode (str/split pair #"=" 2))))))
      base-url)))

(defn combine-url [{url :url, params :query}]
  (if params
    (apply str url "?"
           (->> params
                (map #(interpose "=" (map b/url-encode %)))
                (interpose ["&"])
                (apply concat)))
    url))

(defn set-page! [url]
  (rf/dispatch [:routing/page-changed url]))

(defn parse-url-str [url-str]
  (let [{base :url, query :query} (split-url url-str)
        match (b/match-route u/client-routes base)]
    (if match
      (let [{handler :handler
             params :route-params} match
            url {:handler handler
                 :params params}]
        (if query
          (assoc url :query query)
          url))
      nil)))

(def history
  (p/pushy set-page! parse-url-str))

(defn intercept-routes! []
  (p/start! history))

(defn url-str
  {:arglists '([{handler :handler
                 params :params
                 query :query}])}
  [{query :query
    :as url}]
  (combine-url
    {:url (b/unmatch-pair u/client-routes url)
     :query query}))

(defn handle-update-history
  {:arglists '([{handler :handler
                 params :params
                 query :query}])}
  [url]
  (p/set-token! history (url-str url)))

(rf/reg-fx
  :routing/update-history
  handle-update-history)

(defn handle-navigate [_ [_ url replace]]
  {:routing/update-history (assoc url :replace replace)})

(rf/reg-event-fx
  :navigate
  handle-navigate)

(defmulti handle-route
  "The main route handler for the client.  Takes a cofx and a full url spec"
  {:arglists '([cofx
                {handler :handler
                 params :params
                 query :query}])}
  (fn [_ {handler :handler}] handler))

(defmethod handle-route :client/not-found
  [_ _]
  {:db {:location {:handler :client/not-found}}})

(defn handle-page-changed [cofx [_ url]]
  (handle-route cofx url))

(rf/reg-event-fx
  :routing/page-changed
  handle-page-changed)
