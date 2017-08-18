(ns book-sorter.test
  (:require [book-sorter.test-routing :as routing]
            [cljs.test :refer-macros [run-tests]]))

(defn ^:export run []
  (run-tests 'book-sorter.test-routing))


