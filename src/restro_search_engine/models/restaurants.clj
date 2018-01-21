(ns restro-search-engine.models.restaurants
  (:require [clojurewerkz.elastisch.rest :as cer]
            [clojurewerkz.elastisch.rest.index :as ceri]))


(defonce ^{:doc "Index name for the restaurants index."}
  index-name "restaurants")

(defonce ^{:doc "Index type mapping used in restaurants index."}
  index-type "default")


(defn fetch-restaurant-record
  [{:keys [es-conn]} id]
  (let [record-url (cer/record-url es-conn
                                   index-name
                                   index-type
                                   id)
        restaurant (cer/get es-conn
                            record-url)]
    (:_source restaurant)))


(defn create-record
  [{:keys [es-conn]} record]
  (let [url (cer/mapping-type-url es-conn
                                  index-name
                                  index-type)
        result (cer/post es-conn
                         url
                         {:body record})]
    (assoc record
           :id (:_id result))))


(defn update-record
  [{:keys [es-conn] :as conn} {:keys [id] :as record}]
  (let [existing-document (fetch-restaurant-record conn
                                                   id)
        updated-doc (merge existing-document
                           (dissoc record
                                   :id))
        record-url (cer/record-url es-conn
                                   index-name
                                   index-type
                                   id)]
    (cer/put es-conn
             record-url
             {:body updated-doc})
    (merge existing-document
           record)))
