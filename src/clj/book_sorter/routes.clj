(ns book-sorter.routes
  (:require [ring.middleware.reload :as reload]
            [ring.middleware.resource :as resource]
            [ring.middleware.params :as params]
            [ring.util.response :as response]
            [bidi.bidi :as b]
            [cheshire.core :as c]
            [book-sorter.urls :as u]))

(def book-data
  (atom [{:id 0
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
          :genre :comedy}]))

(defmulti handle-route
  "The main route handler for the server.
    route is the result from bidi matching, and req
    is the ring request.  Returns clojure data structures
    to turn into json"
  {:arglists '([route req])}
  (fn [route req] (:handler route)))

(defn clean-books [books]
  (->> books
       (map #(select-keys % [:id :name :author]))
       (sort-by :author)))

(defmethod handle-route :book/all
  [_ _]
  (clean-books @book-data))

(defmethod handle-route :book/search
  [_ {searches :params}]
  (letfn [(matches [book]
            (every? identity
                    (for [[field search] searches]
                      (let [field (keyword field)
                            book-val (book field)]
                        (or (not book-val)
                            (.contains book-val search))))))]
    (clean-books (filter matches @book-data))))

(defmethod handle-route :book/get
  [{{book-id :book-id} :route-params} _]
  (let [book-id (Integer/parseInt book-id)]
    (some #(and (= (:id %) book-id) %)
          @book-data)))

(defmethod handle-route :server/client-route
  [_ _]
  nil)

(defn make-handler
  "Makes a ring handler given a function that matches routes and a function
that handles routes"
  [match-route handle-route]
  (fn [{uri :uri :as req}]
    (let [route (match-route uri)
          result (handle-route route req)]
      (if result
        {:status 200
         :headers {"Content-Type" "text/json"}
         :body (c/generate-string result)}
        (response/resource-response "public/index.html")))))

(def app
  (-> (make-handler (partial b/match-route u/api-routes) handle-route)
      params/wrap-params
      (resource/wrap-resource "public")))

(def dev-app
  (reload/wrap-reload app {:dirs ["src/clj" "src/cljc"]}))
