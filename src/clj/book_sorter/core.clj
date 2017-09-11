(ns book-sorter.core)

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

(defn clean-books [books]
  (->> books
       (map #(select-keys % [:id :name :author]))
       (sort-by :author)))


(defn find-book [book-id]
  (some #(and (= (:id %) book-id) %)
        @book-data))

(defmulti get-data
  "gets data for a given request"
  {:arglists '([[identifier & arguments]])}
  (fn [[id]] id))

(defmethod get-data :book/all
  [_]
  (clean-books @book-data))

(defmethod get-data :book/get
  [[_ book-id]]
  (or (find-book book-id)
      :book/no-book))
