(ns book-sorter.subscription-manager-test
  (:require [clojure.test :refer [deftest testing is use-fixtures are]]
            [book-sorter.subscription-manager :as sm]))

(deftest check-manager
  (testing "check-manager returns true when correct"
    (is (sm/check-manager {:by-id {:a #{[1 2] [3 4]}
                                   :b #{[1 2] [4 5]}}
                           :by-sub {[1 2] #{:a :b}
                                    [3 4] #{:a}
                                    [4 5] #{:b}}})))
  (testing "check-manager fails when by-id has extra"
    (is (not (sm/check-manager {:by-id {:a #{[1 2] [3 4]}
                                        :b #{[1 2] [4 5]}}
                                :by-sub {[1 2] #{:a :b}
                                         [4 5] #{:b}}}))))
  (testing "check-manager fails when by-sub has extra"
    (is (not (sm/check-manager {:by-id {:a #{[1 2] [3 4]}
                                        :b #{[1 2]}}
                                :by-sub {[1 2] #{:a :b}
                                         [3 4] #{:a}
                                         [4 5] #{:b}}})))))

(let [base {:by-id {:a #{[1 2] [3 4]}
                    :b #{[1 2] [4 5]}}
            :by-sub {[1 2] #{:a :b}
                     [3 4] #{:a}
                     [4 5] #{:b}}}]
  
  (deftest add-sub
    (testing "add-sub works when adding a new sub"
      (let [new (sm/add-sub base :a [7 9])]
        (is (sm/check-manager base))
        (is (sm/check-manager new))
        (is (-> new :by-id (get :a) (get [7 9])))
        (is (-> new :by-id (get :b) (get [7 9]) not))
        (is (-> new :by-sub (get [7 9]) (= #{:a})))))
    (testing "add-sub works when adding a new id"
      (let [new (sm/add-sub base :c [1 2])]
        (is (sm/check-manager base))
        (is (sm/check-manager new))
        (is (-> new :by-id (get :c) (get [1 2])))
        (is (-> new :by-sub (get [1 2]) (get :c)))
        (is (-> new :by-sub (get [3 4]) (get :c) not))
        (is (-> new :by-sub (get [4 5]) (get :c) not))))
    (testing "add-sub works when adding a new combo"
      (let [new (sm/add-sub base :c [7 9])]
        (is (sm/check-manager base))
        (is (sm/check-manager new))
        (is (-> new :by-id (get :c) (get [7 9])))
        (is (-> new :by-id (get :a) (get [7 9]) not))
        (is (-> new :by-id (get :b) (get [7 9]) not))
        (is (-> new :by-sub (get [7 9]) (get :c)))
        (is (-> new :by-sub (get [3 4]) (get :c) not))
        (is (-> new :by-sub (get [4 5]) (get :c) not))))
    (testing "add-sub is a no-op when already subbed"
      (let [new (sm/add-sub base :a [1 2])]
        (is (sm/check-manager base))
        (is (= new base)))))

  (deftest remove-subs
    (testing "remove-subs removes all subs"
      (let [new (sm/remove-subs base :a)]
        (is (sm/check-manager base))
        (is (sm/check-manager new))
        (is (-> new :by-id (get :a) not))
        (is (-> new :by-id (get :b)))
        (is (-> new :by-sub (get [1 2]) (= #{:b})))
        (is (-> new :by-sub (get [4 5]) (= #{:b})))
        (is (-> new :by-sub (contains? [3 4]) not))))
    (testing "remove-subs is a no-op when removing an id without subs"
      (let [new (sm/remove-subs base :c)]
        (is (sm/check-manager base))
        (is (= base new))))))
