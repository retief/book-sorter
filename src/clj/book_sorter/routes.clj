(ns book-sorter.routes
  (:require [ring.middleware.reload :as reload]
            [ring.middleware.resource :as resource]
            [ring.middleware.params :as params]
            [ring.middleware.keyword-params :as keyword-params]
            [ring.util.response :as response]
            [bidi.bidi :as b]
            [cheshire.core :as c]
            [book-sorter.urls :as u]
            [aleph.http :as http]
            [aleph.netty :as netty]
            [book-sorter.core :refer [book-data clean-books find-book]]
            [book-sorter.sente-api :as sa]))



(defn json-response [val]
  (c/generate-string val))

(defmulti handle-route
  "The main route handler for the server.
    route is the result from bidi matching, and req
    is the ring request.  Returns clojure data structures
    to turn into json"
  {:arglists '([route req])}
  (fn [route req] (:handler route)))

(defmethod handle-route :book/all
  [_ _]
  (json-response
    (clean-books @book-data)))

(defmethod handle-route :book/search
  [_ {searches :params}]
  (letfn [(matches [book]
            (every? identity
                    (for [[field search] searches]
                      (let [book-val (book field)]
                        (or (not book-val)
                            (.contains book-val search))))))]
    (json-response
      (clean-books (filter matches @book-data)))))

(defmethod handle-route :book/get
  [{{book-id :book-id} :route-params} _]
  (let [book-id (Integer/parseInt book-id)
        result (find-book book-id)]
    (and result
         (json-response
           result))))



(defmethod handle-route :sente/setup
  [_ {method :request-method :as req}]
  ((case method
     :get sa/ring-ajax-get-or-ws-handshake
     :post sa/ring-ajax-post
     (constantly nil)) req))

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
         :body result #_(c/generate-string result)}
        (response/resource-response "public/index.html")))))

(def app
  (-> (make-handler (partial b/match-route u/api-routes) handle-route)
      keyword-params/wrap-keyword-params
      params/wrap-params
      (resource/wrap-resource "public")))

(def dev-app
  (reload/wrap-reload app {:dirs ["src/clj" "src/cljc"]}))

(defn -main [port]
  (let [port (if port
               (Integer/parseInt port)
               3000)]
    (println "starting server on:" port)
    (netty/wait-for-close
      (http/start-server dev-app {:port port}))))
