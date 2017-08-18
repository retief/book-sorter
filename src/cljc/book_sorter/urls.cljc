(ns book-sorter.urls)

(def client-routes
  ["/" {"" :client/home
        ["book/" [#"\d+" :book-id]] :client/show-book}])

(def api-routes
  ["/api/" {"book/search" :book/search
            "book/all" :book/all 
            ["book/" [#"\d+" :book-id]] :book/get}])

