(ns book-sorter.routes-test
  (:require [clojure.test :refer [deftest testing is use-fixtures are]]
            [book-sorter.routes :as r]
            [book-sorter.urls :as u]
            [bidi.bidi :as b]
            [ring.mock.request :as mock]
            [cheshire.core :as c]))

(def test-data
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
    :genre :sci-fi}])

(defn reset-data [f]
  (let [old @book-data]
    (reset! book-data test-data)
    (f)
    (reset! book-data old)))

(use-fixtures :each reset-data)

(deftest test-routing
  (testing "general tests"
    (is (= (disj (set (keys (methods handle-route))) :default)
           (set (map :handler (b/route-seq u/api-routes))))
        "all routes are handled, all handlers have routes (not counting default)"))

  (testing ":books/all"
    (is (= (handle-route {:handler :book/all} nil)
           [{:id 0
             :name "Young Miles"
             :author "Bujold, Lois McMaster"}
            {:id 4
             :name "With the Lightnings"
             :author "Drake, David"}
            {:id 1
             :name "Pawn of Prophecy"
             :author "Eddings, David"}
            {:id 2
             :name "Good Omens"
             :author "Pratchett, Terry and Neil Gaiman"}
            {:id 3
             :name "On Basilisk Station"
             :author "Weber, David"}]))
    (is (= (:handler (b/match-route u/api-routes "/api/book/all"))
           :book/all)))

  (testing ":book/search"
    (are [params result] (= (handle-route {:handler :book/search}
                                          {:params params})
                            result)
      {"name" "Young"}
      [{:id 0
        :name "Young Miles"
        :author "Bujold, Lois McMaster"}]

      {"author" "David"
       "name" "Pawn"}
      [{:id 1
        :name "Pawn of Prophecy"
        :author "Eddings, David"}]
      
      {"author" "David"}
      [{:id 4
        :name "With the Lightnings"
        :author "Drake, David"}
       {:id 1
        :name "Pawn of Prophecy"
        :author "Eddings, David"}
       {:id 3
        :name "On Basilisk Station"
        :author "Weber, David"}]

      {"author" "foo"}
      []

      {"foo" "bar"}
      [{:id 0
        :name "Young Miles"
        :author "Bujold, Lois McMaster"}
       {:id 4
        :name "With the Lightnings"
        :author "Drake, David"}
       {:id 1
        :name "Pawn of Prophecy"
        :author "Eddings, David"}
       {:id 2
        :name "Good Omens"
        :author "Pratchett, Terry and Neil Gaiman"}
       {:id 3
        :name "On Basilisk Station"
        :author "Weber, David"}]

      {"foo" "bar"
       "author" "David"}
      [{:id 4
        :name "With the Lightnings"
        :author "Drake, David"}
       {:id 1
        :name "Pawn of Prophecy"
        :author "Eddings, David"}
       {:id 3
        :name "On Basilisk Station"
        :author "Weber, David"}])

    (is (:handler (b/match-route u/api-routes "/api/book/search"))
        :book/search))
  
  (testing ":get-book"
    (is (= (handle-route {:handler :book/get :route-params {:book-id "1"}} nil)
           {:id 1
            :name "Pawn of Prophecy"
            :author "Eddings, David"
            :description "Young Garion and his aunt get caught up in historic events"
            :genre :fantasy})
        "getting a book works")
    (is (= (handle-route {:handler :book/get :route-params {:book-id "100"}} nil)
           nil)
        "getting a non-existant book returns nil")
    (is (= (b/match-route u/api-routes "/api/book/5")
           {:handler :book/get :route-params {:book-id "5"}})
        "route with numeric id works")
    (is (= (b/match-route u/api-routes "/api/book/asdf")
           nil)
        "route with non-numeric id returns nil")))

(deftest test-make-handler
  (is (instance? java.io.File
                 (:body ((make-handler (constantly nil) (fn [x _] x))
                         (mock/request :get "/foo/bar")))))
  (is (instance? java.io.File
                 (:body ((make-handler identity (constantly nil))
                         (mock/request :get "/foo/bar")))))
  (is (.contains (:body ((make-handler identity (fn [x _] x))
                         (mock/request :get "/foo/bar")))
                 "/foo/bar")))

(deftest test-app
  (is (= (-> (mock/request :get "/api/book/all")
             app
             :body
             (c/parse-string true))
         [{:id 0
           :name "Young Miles"
           :author "Bujold, Lois McMaster"}
          {:id 4
           :name "With the Lightnings"
           :author "Drake, David"}
          {:id 1
           :name "Pawn of Prophecy"
           :author "Eddings, David"}
          {:id 2
           :name "Good Omens"
           :author "Pratchett, Terry and Neil Gaiman"}
          {:id 3
           :name "On Basilisk Station"
           :author "Weber, David"}]))
  (is (= (-> (mock/request :get "/api/book/search?author=David")
             app
             :body
             (c/parse-string true))
         [{:id 4
           :name "With the Lightnings"
           :author "Drake, David"}
          {:id 1
           :name "Pawn of Prophecy"
           :author "Eddings, David"}
          {:id 3
           :name "On Basilisk Station"
           :author "Weber, David"}]))

  (is (= (-> (mock/request :get "/index.html")
             app
             :status)
         200))
  (is (= (-> (mock/request :get "/index.html")
             app
             :body)
         (-> (mock/request :get "/")
             app
             :body)
         (-> (mock/request :get "/book/1")
             app
             :body))))
