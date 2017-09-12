(ns book-sorter.subscription-manager)

(def base-manager
  {:by-id {}
   :by-sub {}})

(defn add-to-set [s v]
  (if s
    (conj s v)
    #{v}))

(defn remove-key [mp key val]
  (let [new-val-list (disj (mp key) val)]
    (if (empty? new-val-list)
      (dissoc mp key)
      (assoc mp key new-val-list))))

(defn remove-subs-by-id [{:keys [by-id by-sub]}
                   id]
  {:by-id (dissoc by-id id)
   :by-sub (let [subs (by-id id)]
             (reduce #(remove-key %1 %2 id) by-sub subs))})

(defn remove-sub [{:keys [by-id by-sub]}
                  id sub]
  {:by-id (remove-key by-id id sub)
   :by-sub (remove-key by-sub sub id)})

(defn remove-subs [manager id subs]
  (reduce #(remove-sub %1 id %2) manager subs))

(defn add-sub [{:keys [by-id by-sub]}
               id sub]
  {:by-id (update by-id id (fnil conj #{}) sub)
   :by-sub (update by-sub sub (fnil conj #{}) id)})

(defn add-subs [manager id requests]
  (reduce #(add-sub %1 id %2) manager requests))

(defn check-manager [{:keys [by-id by-sub]}]
  (let [make-pairs (fn [[k vs]]
                     (map #(vector k %) vs))
        flip-pair (fn [[a b]] [b a])]
    (= (set (mapcat make-pairs by-id))
       (set (mapcat #(->> % make-pairs (map flip-pair))
                    by-sub)))))
