(ns memwise.db.transaction
  (:require [datomic.api :as d]
            [clojure.string :as str]))

(def ^:dynamic conn nil)

(defn add-new-deck-tx [deck-name nr-of-fields]
  [{:db/id (d/tempid :db.part/user)
    :deck/name deck-name
    :deck/nrOfFields nr-of-fields}])

(defn add-new-deck [deck-name nr-of-fields]
  @(d/transact conn (add-new-deck-tx deck-name nr-of-fields)))

(defn add-card-to-deck-tx [deck-name {:keys [fields] :as card}]
  [{:db/id [:deck/name deck-name] :deck/card #db/id [db.part/user -1]}
   {:db/id #db/id [db.part/user -1] :card/field fields}])

(defn add-card-to-deck [deck-name card]
  @(d/transact conn (add-card-to-deck-tx deck-name card)))

(defn remove-card-tx [deck-name card-number]
  [:db/retractEntity [:card/number card-number]])

(defn select-decks [db]
  (d/q '[:find ?deck ?nr-of-fields
         :where
         [?e :deck/name ?deck]
         [?e :deck/nrOfFields ?nr-of-fields]] db))

(defn select-deck-by-name [db deck-name]
  (d/q '[:find ?nr-of-fields
         :in $ ?deck-name
         :where
         [?e :deck/name ?deck-name]
         [?e :deck/nrOfFields ?nr-of-fields]]
       db deck-name))

(defn add-new-card-to-deck [db deck-name {:keys [fields] :as card}]
  (let [nr-of-fields (ffirst (select-deck-by-name db deck-name))
        nr-of-fields-given (count fields)]
    (if (= nr-of-fields nr-of-fields-given)
      (add-card-to-deck deck-name card)
      (throw (IllegalArgumentException. (str/join " " ["Deck has" nr-of-fields "fields." nr-of-fields-given "given."]))))))

(defn select-cards-from-deck [db deck-name]
  (d/q '[:find ?card
         :in $ ?deck-name
         :where
         [?card :card/field ?field]
         [?deck :deck/card ?card]
         [?deck :deck/name ?deck-name]]
       db deck-name))

(defn select-card-fields [db card]
  (d/q '[:find ?field
         :in $ ?card
         :where
         [?card :card/field ?field]]
       db card))

(defn select-card [db card]
  (transduce (map first) conj #{} (select-card-fields db card)))

