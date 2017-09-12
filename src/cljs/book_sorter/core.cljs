(ns book-sorter.core
  (:require [reagent.core :as reagent]
            [re-frame.core :as rf]
            [reagent.core :as re]
            [reagent.ratom :as ra]
            [clojure.string :as str]
            [book-sorter.routing :as r]
            [book-sorter.sente :as bs]))

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

(rf/reg-sub
  :location
  (fn [{location :location} _]
    location))

(rf/reg-sub
  :handler
  :<- [:location]
  (fn [{:keys [handler]} _]
    handler))

(rf/reg-sub
  :page-data
  (fn [{data :page-data} _]
    data))

(rf/reg-event-db
  :client/update-subs
  (fn [db [_ subs]]
    (update db :subscriptions merge subs)))

(rf/reg-event-db
  :client/clear-sub
  (fn [db [_ sub]]
    (update db :subscriptions dissoc sub)))

(rf/reg-sub-raw
  :api-data
  (fn [app-db [_ sub]]
    (bs/send-message! {:event [:book/data [sub]]
                       :on-success [:client/update-subs]
                       :on-failure [:client/sente-fail]})
    (ra/make-reaction
      #(get-in @app-db [:subscriptions sub])
      :on-dispose #(do
                     (rf/dispatch [:client/clear-sub sub])
                     (bs/send-message! {:event [:book/clear-subs [sub]]})))))

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

(defn ui []
  (show-page @(rf/subscribe [:location])))

(defn ^:export run
  []
  (rf/dispatch-sync [:initialize])
  (reagent/render [ui]
                  (js/document.getElementById "app"))
  (r/intercept-routes!))

;; client/home

(rf/reg-sub
  :client.home/query
  :<- [:handler]
  :<- [:page-data]
  (fn [[handler query] _]
    (and (= handler :client/home) query)))

(rf/reg-sub
  :client.home/filtered-books
  :<- [:client.home/query]
  :<- [:api-data [:book/all]]
  (fn [[query data] _]
    (when query
      (let [f (fn [book]
                (every?
                  (fn [[key search-value]]
                    (if-let [book-value (key book)]
                      (not (= (.indexOf book-value search-value) -1))
                      true))
                  query))]
        (filter f data)))))

(rf/reg-event-db
  :client.home/update-query
  (fn [db [_ k v]]
    (if (-> db :location :handler (= :client/home))
      (assoc-in db [:page-data k] v))))

(defmethod r/handle-route :client/home
  [{:keys [db]} url]
  {:db (assoc db :location url :page-data {:name "" :author ""})})

(defn show-book [{name :name
                  author :author
                  id :id}]
  [:div
   [:a.details {:href (r/url-str {:handler :client/show-book
                                  :params {:book-id id}})}
    [:span.name name] " by " [:span.author author]]])

(defmethod show-page :client/home
  [_]
  (let [book-list @(rf/subscribe [:client.home/filtered-books])
        query @(rf/subscribe [:client.home/query])]
    [:div [:h1 "all books"]
     [:div
      [:label {:for "name-query"} "Name: "]
      [:input#name-query {:type "text"
                          :value (:name query)
                          :on-change #(rf/dispatch [:client.home/update-query
                                                    :name
                                                    (-> % .-target .-value)])}]
      [:label {:for "author-query"} "Author: "]
      [:input#author-query {:type "text"
                            :value (:author query)
                            :on-change #(rf/dispatch [:client.home/update-query
                                                      :author
                                                      (-> % .-target .-value)])}]]
     (for [book book-list]
       ^{:key (:id book)} [show-book book])]))

;; client/show-book

(rf/reg-sub
  :client.show-book/book-data
  (fn [_]
    (let [page-data (rf/subscribe [:page-data])]
      [page-data (rf/subscribe [:api-data [:book/get (:id @page-data)]])]))
  (fn [[id book] _] book))

(rf/reg-event-fx
  :client.show-book/tag-book
  (fn [{:keys [db]} [_ tag]]
    (let [id (-> db :page-data :id)]
      {:sente/send {:event [:book/set-tag
                            {:id id
                             :tag tag}]}})))

(defmethod r/handle-route :client/show-book
  [{:keys [db]} {{id :book-id} :params
               :as url}]
  (let [id (js/parseInt id 10)]
    {:db (assoc db
                :page-data {:id id}
                :location url)}))

(defmethod show-page :client/show-book
  [_]
  (let [{:keys [name author description genre tag]}
        @(rf/subscribe [:client.show-book/book-data])]
    [:div
     [:div name]
     [:div author]
     [:div description]
     [:div genre]
     [:div
      [:label {:for "tag"} "Tag: "]
      [:input#tag {:type "text"
                   :value tag
                   :on-change #(rf/dispatch
                                 [:client.show-book/tag-book
                                  (-> % .-target .-value)])}]]
     [:a {:href (r/url-str {:handler :client/home})} "Up"]]))
