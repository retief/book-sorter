(ns book-sorter.subscription-manager)

(def base-manager
  {:by-id {}
   :by-sub {}})

(defn add-to-set [s v]
  (if s
    (conj s v)
    #{v}))

(defn remove-subs [{:keys [by-id by-sub]}
                   id]
  {:by-id (dissoc by-id id)
   :by-sub (let [subs (by-id id)
                 remove-id (fn [by-sub sub]
                             (let [new-id-list (disj (by-sub sub) id)]
                               (if (empty? new-id-list)
                                 (dissoc by-sub sub)
                                 (assoc by-sub sub new-id-list))))]
             (reduce remove-id by-sub subs))})

(defn add-sub [{:keys [by-id by-sub]}
               id sub]
  {:by-id (update by-id id (fnil conj #{}) sub)
   :by-sub (update by-sub sub (fnil conj #{}) id)})

(defn check-manager [{:keys [by-id by-sub]}]
  (let [make-pairs (fn [[k vs]]
                     (map #(vector k %) vs))
        flip-pair (fn [[a b]] [b a])]
    (= (set (mapcat make-pairs by-id))
       (set (mapcat #(->> % make-pairs (map flip-pair))
                    by-sub)))))
