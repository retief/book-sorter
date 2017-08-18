(ns book-sorter.test
  (:require [book-sorter.routing-test :as routing]
            [cljs.test :refer-macros [run-tests]]))

(defn ^:export run []
  (run-tests 'book-sorter.routing-test))


