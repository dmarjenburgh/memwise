(ns cladura.dal.transaction
  (:require [datomic.api :as d]
            [clojure.string :as str]))

(def ^:dynamic conn nil)

(defn add-new-deck-tx [deck-name nr-of-fields]
  [{:db/id (d/tempid :db.part/user)
    :deck/name deck-name
    :deck/nrOfFields nr-of-fields}])

(defn add-new-deck [deck-name]
  @(d/transact conn (add-new-deck-tx deck-name)))

(defn add-card-to-deck-tx [deck-name {:keys [fields] :as card}]
  (letfn [(field-tx [tempid content]
            {:db/id (d/tempid :db.part/user tempid)
             :field/card #db/id [:db.part/user -1]
             :field/content content})
          (field-txs [fields]
            (map-indexed (fn [index content]
                           (field-tx (- (+ index 2)) content)) fields))]
    (into [{:db/id #db/id[:db.part/user -1] :card/deck [:deck/name deck-name]}]
      (field-txs fields))))

(defn add-card-to-deck [deck-name card]
  @(d/transact conn (add-card-to-deck-tx deck-name card)))

(defn remove-card-tx [deck-name card-number]
  [:db/retractEntity [:card/number card-number]])

(def select-decks-q
  '[:find ?deck ?nr-of-fields
    :where
    [?e :deck/name ?deck]
    [?e :deck/nrOfFields ?nr-of-fields]])

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
         [?card :card/deck ?deck]
         [?deck :deck/name ?deck-name]]
       db deck-name))

(defn select-card-fields [db card]
  (mapv second
        (sort-by first (d/q '[:find ?field ?content
                              :in $ ?card
                              :where
                              [?field :field/card ?card]
                              [?field :field/content ?content]]
                            db card))))
