(ns book-sorter.core
  (:require [reagent.core :as reagent]
            [re-frame.core :as rf]
            [clojure.string :as str]
            [book-sorter.routing :as r]
            [book-sorter.sente]))


(def book-data
  [{:id 0
    :name "Young Miles"
    :author "Bujold, Lois McMaster"
    :description "Miles accidentally a mercenary fleet"
    :genre :sci-fi}
   {:id 1
    :name "Pawn of Prophecy"
    :author "Eddings, David"
    :description "Young Garion and his aunt get caught up in historic events"
    :genre :fantasy}
   {:id 2
    :name "Good Omens"
    :author "Pratchett, Terry and Neil Gaiman"
    :description "What if the antichrist was a 10 year old kid?"
    :genre :fantasy}
   {:id 3
    :name "On Basilisk Station"
    :author "Weber, David"
    :description "Honor Harrington is handed a ship with an \"experimental\"
loadout and a crew that hates her.  Can she foil the dastardly Havenite plot?"
    :genre :sci-fi}
   {:id 4
    :name "With the Lightnings"
    :author "Drake, David"
    :description "Daniel Leary and Adele Mundy must team up to save the planet
from the evil Alliance"
    :genre :sci-fi}
   {:id 5
    :name "My Family and Other Animals"
    :author "Durrell, Gerald"
    :description "10 year old Gerald Durrell moves to Corfu with his family"
    :genre :comedy}])

(rf/reg-event-fx
  :initialize
  (fn [_ _]
    {:db {:data book-data}}))

(defmethod r/handle-route :client/home
  [{{data :data
     :as db} :db} url]
  {:db (assoc db
              :page-data data
              :location url)})

(defmethod r/handle-route :client/show-book
  [{{data :data
     :as db} :db} {{id :book-id} :params
                   :as url}]
  (let [id (js/parseInt id 10)
        book (some #(and (= (:id %) id) %) data)]
    {:db (if book
           (assoc db
                  :page-data book
                  :location url)
           (assoc db
                  :page-data url
                  :location {:handler :client/not-found}))
     :sente/send {:event [:foo/bar "hello"]
                  :on-success [:client/sente-responded]
                  :on-failure [:client/sente-responded]}}))

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
  [_ book-list]
  [:div [:h1 "all books"]
   (for [book book-list]
     ^{:key (:id book)} [show-book book])])

(defmethod show-page :client/show-book
  [_ {name :name
      author :author
      description :description
      genre :genre}]
  [:div
   [:div name]
   [:div author]
   [:div description]
   [:div genre]
   [:a {:href (r/url-str {:handler :client/home})} "Up"]])

(defn ui []
  (show-page @(rf/subscribe [:location])
             @(rf/subscribe [:page-data])))

(defn ^:export run
  []
  (rf/dispatch-sync [:initialize])
  (reagent/render [ui]
                  (js/document.getElementById "app"))
  (r/intercept-routes!))
