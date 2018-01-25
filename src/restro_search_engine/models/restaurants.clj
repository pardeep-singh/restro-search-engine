(ns restro-search-engine.models.restaurants
  (:require [clojurewerkz.elastisch.rest :as cer]
            [clojurewerkz.elastisch.rest.index :as ceri]
            [clojure.set :refer [rename-keys]]))


(defonce ^{:doc "Index name for the restaurants index."}
  index-name "restaurants")

(defonce ^{:doc "Index type mapping used in restaurants index."}
  index-type "default")

(defonce index-settings
  {:index {:number_of_replicas 1
           :number_of_shards 1
           :refresh_interval "5s"}})

(defonce ^{:doc "Restaurants Index Mappings"}
  index-mappings {:default {:properties {:title {:type "text"}
                                         :email {:type "keyword"}
                                         :phone_number {:type "keyword"}
                                         :veg_only {:type "boolean"}
                                         :favourite_counts {:type "long"}
                                         :delivery_only {:type "long"}
                                         :ratings {:type "integer"}
                                         :expected_delivery_duration {:type "integer"}
                                         :location {:type "geo_point",
                                                    :ignore_malformed true}
                                         :menu_list {:type "nested"
                                                     :properties {:dish_name {:type "text"}
                                                                  :veg {:type "boolean"}
                                                                  :price {:type "integer"}
                                                                  :category {:type "keyword"}
                                                                  :expected_preparation_duration {:type "integer"}}}}}})


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


(defmulti build-query (fn [field _]
                        (keyword field)))


(defmethod build-query :title
  [_ value]
  {:match {:title value}})


(defmethod build-query :veg_only
  [_ value]
  {:term {:veg_only value}})


(defmethod build-query :delivery_only
  [_ value]
  {:term {:delivery_only value}})


(defmethod build-query :favourite_counts
  [_ query]
  (let [range_query (-> query
                        (select-keys [:less_than_equal_to :greater_than_equal_to])
                        (rename-keys {:less_than_equal_to :lte
                                      :greater_than_equal_to :gte}))]
    {:range {:favourite_counts range_query}}))


(defmethod build-query :expected_delivery_duration
  [_ query]
  (let [range_query (-> query
                        (select-keys [:less_than_equal_to :greater_than_equal_to])
                        (rename-keys {:less_than_equal_to :lte
                                      :greater_than_equal_to :gte}))]
    {:range {:favourite_counts range_query}}))


(defn build-es-query
  [query]
  (let [query-context-fields (select-keys query
                                          [:title])
        filter-context-fields (select-keys query
                                           [:veg_only :delivery_only :favourite_counts :expected_delivery_duration])
        construct-queries-fn (fn [acc k v]
                               (conj acc
                                     (build-query k v)))
        es-queries (reduce-kv construct-queries-fn
                              []
                              query-context-fields)
        es-filters (reduce-kv construct-queries-fn
                              []
                              filter-context-fields)]
    {:query {:bool {:must es-queries
                    :filter {:bool {:must es-filters}}}}}))


(defn search-restaurants
  [{:keys [es-conn]} {:keys [query sort-field sort-order]
                      :or {sort-field "_score"
                           sort-order "desc"}}]
  (let [es-query (build-es-query query)
        sorting-object {(keyword sort-field) {:order sort-order}}
        final-query (assoc es-query
                           :sort sorting-object)
        search-url (cer/search-url es-conn
                                   index-name
                                   index-type)]
    (cer/post es-conn
              search-url
              {:body final-query})))
