(ns cladura.dal.transaction-test
  (:require [midje.sweet :refer :all]
            [cladura.dal.transaction :refer :all]
            [datomic.api :as d]))

(def schema-file (load-file "resources/datomic/schema.edn"))

(defn- create-empty-db []
  (let [uri "datomic:mem://hello"]
    (d/delete-database uri)
    (d/create-database uri)
    (let [conn (d/connect uri)]
      (d/transact conn schema-file)
      conn)))

(def base-db (d/db (create-empty-db)))

(facts "About decks"
  (let [db base-db]
    (fact "Initially, there are no decks"
      (select-decks db) => empty?))
  (fact "We can add new decks"
    (let [new-deck-name "some-test-deck"
          nr-of-fields 2
          {no-decks-db :db-before one-deck-db :db-after} (d/with base-db (add-new-deck-tx new-deck-name nr-of-fields))]
      (select-decks no-decks-db) => empty?
      (select-decks one-deck-db) => #{[new-deck-name nr-of-fields]}
      (let [{:keys [db-before db-after]} (d/with one-deck-db (add-new-deck-tx "another test deck" nr-of-fields))]
        (select-decks db-before) => #{["some-test-deck" nr-of-fields]}
        (select-decks db-after) => #{["some-test-deck" nr-of-fields] ["another test deck" nr-of-fields]})))
  (fact "Deck names are unique"
    (d/with (:db-after (d/with base-db (add-new-deck-tx "deckname" 2)))
            (add-new-deck-tx "deckname" 3)) => (throws IllegalStateException)))

(facts "About cards"
  (let [deck-name "A new test deck"
        nr-of-fields 2
        db (:db-after (d/with base-db (add-new-deck-tx deck-name nr-of-fields)))]
    (fact "New decks have no cards"
      (select-cards-from-deck db deck-name) => empty?)
    (fact "We can add new cards"
      (add-new-card-to-deck db deck-name {:fields ["Field 1" "Field 2" "Field 3"]}) => (throws IllegalArgumentException #"2.*3")
      (let [new-card {:fields ["Field 1" "Field 2"]}
            db-with-card (:db-after (d/with db (add-card-to-deck-tx deck-name new-card)))
            cards (select-cards-from-deck db-with-card deck-name)
            contents (select-card-fields db-with-card (ffirst cards))]
        (count cards) => 1
        contents => ["Field 1" "Field 2"]))))
