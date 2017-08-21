(ns book-sorter.routing-test
  (:require [book-sorter.routing :as r]
            [cljs.test :refer-macros [deftest is testing run-tests]]
            [book-sorter.urls :as u]
            [bidi.bidi :as b]
            [clojure.set :as set]))

(deftest test-urls
  (let [test-urls
        [["http://google.com" {:url "http://google.com"}]
         ["http://google.com/a/b/c" {:url "http://google.com/a/b/c"}]
         ["http://google.com/a/b/c/" {:url "http://google.com/a/b/c/"}]
         ["http://google.com?a=b" {:url "http://google.com"
                                   :query {"a" "b"}}]
         ["http://google.com/a/b/c?a=b" {:url "http://google.com/a/b/c"
                                         :query {"a" "b"}}]
         ["http://google.com/a/b/c/?a=b" {:url "http://google.com/a/b/c/"
                                          :query {"a" "b"}}]
         ["http://google.com/a/b/c?a=b&c=d" {:url "http://google.com/a/b/c"
                                             :query {"a" "b", "c" "d"}}]
         [(str "http://google.com/?"
               "is%20foo%20%3D%20to%20bar%3F="
               "I'm%20pretty%20sure%20that%20it%20isn't%2C%20but%20you%20cannot%20be%20sure")
          {:url "http://google.com/"
           :query {"is foo = to bar?" "I'm pretty sure that it isn't, but you cannot be sure"}}]
         (let [really-long-url
               (apply str "http://google.com" (repeat 100 "hello/"))]
           [really-long-url {:url really-long-url}])]]
    (is (= (r/split-url "http://google.com/a/b/c?a=b")
           {:url "http://google.com/a/b/c"
            :query {"a" "b"}}))
    (doseq [[url split] test-urls]
      (is (= (r/split-url url) split))
      (is (= (r/combine-url split) url))
      (is (= (r/combine-url (r/split-url url)) url))
      (is (= (r/split-url (r/combine-url split)) split)))
    (let [url (->> (range 100)
                   (map #(str % "=foo"))
                   (interpose "&")
                   (apply str "http://google.com/?")) 
          split {:url "http://google.com/"
                 :query (into {} (for [i (range 100)]
                                   [(str i) "foo"]))}]
      (is (= (r/split-url url) split))
      (is (= (r/split-url (r/combine-url split)) split)))))

(deftest test-routes
  (let [handler-methods (set (keys (methods r/handle-route)))
        route-methods (set (map :handler (b/route-seq u/client-routes)))]
    (is (empty? (set/difference route-methods handler-methods)))
    (is (empty? (set/difference handler-methods route-methods)))))

(deftest test-parse-url-str
  (is (= (r/parse-url-str "/foo/bar/bash")
         {:handler :client/not-found}))
  (is (= (r/parse-url-str "/foo/bar/bash?a=b&c=d")
         {:handler :client/not-found
          :query {"a" "b" "c" "d"}})))

(deftest test-url-str
  (is (= (r/url-str {:handler :client/home})
         "/"))
  (is (= (r/url-str {:handler :client/show-book
                     :params {:book-id 1}})
         "/book/1"))
  (is (= (r/url-str {:handler :client/show-book
                     :params {:book-id 1}
                     :query {"foo" "bar"}})
         "/book/1?foo=bar")))

(deftest test-handle-navigate
  (is (= (r/handle-navigate nil [:navigate {:handler :test}])
         {:routing/update-history {:handler :test :replace nil}}))
  (is (= (r/handle-navigate nil [:navigate {:handler :test} true])
         {:routing/update-history {:handler :test :replace true}})))

(deftest test-handle-not-found-route
  (is (= (r/handle-route nil {:handler :client/not-found})
         {:db {:location {:handler :client/not-found}}}))
  (is (= (b/match-route u/client-routes "/foo/bar/bash")
         {:handler :client/not-found})))

(deftest test-handle-page-changed
  (is (= (r/handle-page-changed nil [nil {:handler :client/not-found}])
         {:db {:location {:handler :client/not-found}}})))
