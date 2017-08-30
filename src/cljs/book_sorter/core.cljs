(ns book-sorter.core
  (:require [reagent.core :as reagent]
            [re-frame.core :as rf]
            [clojure.string :as str]
            [book-sorter.routing :as r]
            [book-sorter.sente]))

(rf/reg-event-fx
  :initialize
  (fn [_ _]
    {:db {}}))

(rf/reg-event-db
  :client/sente-fail
  (fn [db [_ msg]]
    (println "sente failed")
    (prn msg)
    db))

(rf/reg-event-db
  :client.home/data-loaded
  (fn [db [_ url data]]
    (assoc db
           :page-data {:data data
                       :query {}}
           :location url)))

(rf/reg-event-db
  :client.home/filter
  (fn [db [_ query]]
    (assoc db :filter query)))

(defmethod r/handle-route :client/home
  [_ url]
  {:sente/send {:event [:book/all]
                :on-success [:client.home/data-loaded url]
                :on-failure [:client/sente-fail]}})

(rf/reg-event-db
  :client.show-book/data-loaded
  (fn [db [_ url book]]
    (if book
      (assoc db
             :page-data book
             :location url)
      (assoc db
             :page-data url
             :location {:handler :client/not-found}))))

(defmethod r/handle-route :client/show-book
  [_ {{id :book-id} :params
      :as url}]
  (let [id (js/parseInt id 10)]
    {:sente/send {:event [:book/get id]
                  :on-success [:client.show-book/data-loaded url]}}))

(rf/reg-event-fx
  :client/sente-responded
  (fn [_ event]
    (prn "sente responded" event)))

(rf/reg-sub
  :location
  (fn [{location :location} _]
    location))

(rf/reg-sub
  :page-data
  (fn [{data :page-data} _]
    data))

(rf/reg-sub
  :client.home/filtered-books
  (fn [{{data :data
         query :query} :page-data} _]
    (let [f (fn [book]
              (every?
                (fn [[key search-value]]
                  (if-let [book-value (key book)]
                    (not (= (.indexOf book-value search-value) -1))
                    true))
                query))]
      (filter f data))))

(defmulti show-page
  "Given a location, computes the view
  returns re-frames pseudo-hiccup"
  {:arglists '([{handler :handler
                 params :params
                 query :query} page-data])}
  (fn [{handler :handler} _] handler))

(defmethod show-page :default
  [_]
  [:div "Loading"])

(defmethod show-page :client/not-found
  [_ url]
  [:div
   "Page "
   (r/url-str url)
   " not found"])

(defn show-book [{name :name
                  author :author
                  id :id}]
  [:div
   [:a.details {:href (r/url-str {:handler :client/show-book
                                  :params {:book-id id}})}
    [:span.name name] " by " [:span.author author]]])

(defmethod show-page :client/home
  [_]
  (let [book-list @(rf/subscribe [:client.home/filtered-books])]
    [:div [:h1 "all books"]
     (for [book book-list]
       ^{:key (:id book)} [show-book book])]))

(defmethod show-page :client/show-book
  [_]
  (let [{name :name
         author :author
         description :description
         genre :genre} @(rf/subscribe [:page-data])]
    [:div
     [:div name]
     [:div author]
     [:div description]
     [:div genre]
     [:a {:href (r/url-str {:handler :client/home})} "Up"]]))

(defn ui []
  (show-page @(rf/subscribe [:location])))

(defn ^:export run
  []
  (rf/dispatch-sync [:initialize])
  (reagent/render [ui]
                  (js/document.getElementById "app"))
  (r/intercept-routes!))
