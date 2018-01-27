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

(defonce ^{:doc "Restaurants Index Mappings and settings"}
  index-settings-mappings {:settings {:number_of_replicas 1
                                      :number_of_shards 1
                                      :refresh_interval "5s"}
                           :mappings {:default {:properties {:title {:type "text"}
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
                                                                                      :expected_preparation_duration {:type "integer"}}}}}}})


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
                        field))


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


;; results shouldnt contain fields doc with ratings from 1,2,3 if range is given as 4-5
(defmethod build-query :ratings
  [_ query]
  (let [range_query (-> query
                        (select-keys [:less_than_equal_to :greater_than_equal_to])
                        (rename-keys {:less_than_equal_to :lte
                                      :greater_than_equal_to :gte}))]
    {:range {:ratings range_query}}))


(defmethod build-query :menu_list.dish_name
  [_ value]
  {:nested {:path "menu_list"
            :query {:match {:menu_list.dish_name value}}}})


(defmethod build-query :veg
  [_ value]
  {:term {:menu_list.veg value}})


(defmethod build-query :category
  [_ value]
  {:term {:menu_list.category value}})


(defmethod build-query :menu_list
  [_ value]
  {:nested {:path "menu_list"
             :query {:bool {:must value}}}})


(defmethod build-query :location
  [_ {:keys [distance lat lot]
      :or {distance "100km"}}]
  {:geo_distance {:distance distance
                  :pin.location {:lat lat
                                 :lot lot}}})


(defmulti build-aggregations (fn [k]
                               (keyword k)))


(defmethod build-aggregations :veg_only
  [_]
  {:veg_only {:terms {:field "veg_only"}}})


(defmethod build-aggregations :delivery_only
  [_]
  {:delivery_only {:terms {:field "delivery_only"}}})


(defmethod build-aggregations :ratings
  [_]
  {:ratings {:stats {:field "ratings"}}})


(defmethod build-aggregations :price
  [_]
  {:menu_list {:nested {:path "menu_list"}
               :aggs {:price {:stats {:field "menu_list.price"}}}}})


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
                              filter-context-fields)
        menulist_filters (reduce-kv (fn [acc k v]
                                      (conj acc
                                            (build-query k v)))
                                    []
                                    (select-keys (:menu_list query)
                                                 [:veg :category]))
        filters (conj es-filters
                      (build-query :menu_list menulist_filters))
        queries (if (seq (get-in query
                                 [:menu_list :dish_name]))
                  (->> (get-in query
                               [:menu_list :dish_name])
                       (build-query :menu_list.dish_name)
                       (conj es-queries))
                  es-queries)]
    {:query {:bool {:must queries
                    :filter {:bool {:must filters}}}}}))


(defn search-restaurants
  [{:keys [es-conn]} {:keys [query sort-field sort-order fields page page-size aggs]
                      :or {sort-field "_score"
                           sort-order "desc"
                           fields []
                           page 1
                           page-size 10
                           aggs {}
                           query {}}}]
  (let [es-query (if (seq query)
                   (build-es-query query)
                   {})
        aggs-query (if (seq aggs)
                     (reduce (fn [m k]
                               (-> k
                                   build-aggregations
                                   (merge m)))
                             {}
                             (set aggs))
                     {})
        sorting-object (if (= sort-field "ratings")
                         {(keyword sort-field) {:order sort-order :mode "avg"}}
                         {(keyword sort-field) {:order sort-order}})
        from-param (-> page
                       dec
                       (* page-size))
        final-query (assoc es-query
                           :sort sorting-object
                           :_source fields
                           :from from-param
                           :size page-size
                           :aggs aggs-query)
        search-url (cer/search-url es-conn
                                   index-name
                                   index-type)
        results (cer/post es-conn
                          search-url
                          {:body final-query})
        total-hits (get-in results [:hits :total])
        total-pages (if (= (mod total-hits page-size) 0)
                      (/ total-hits page-size)
                      (-> total-hits
                          (/ page-size)
                          int
                          inc))]
    {:total_hits (get-in results [:hits :total])
     :total_pages total-pages
     :hits (mapv (fn [{:keys [_id _source]}]
                   (assoc _source
                          :id _id))
                 (get-in results [:hits :hits]))
     :aggregations (:aggregations results {})}))


(defn add-ratings
  [es-conn {:keys [id rating]}]
  (let [existing-record (fetch-restaurant-record es-conn id)
        updated-record (update existing-record
                               :ratings conj rating)
        record-url (cer/record-url (:es-conn es-conn)
                                   index-name
                                   index-type
                                   id)]
    (cer/put (:es-conn es-conn)
             record-url
             {:body updated-record})
    (assoc updated-record
           :id id)))


(defn add-dish
  [es-conn zmap]
  (let [existing-record (fetch-restaurant-record es-conn (:id zmap))
        updated-record (update existing-record
                               :menu_list conj (dissoc zmap
                                                       :id))
        record-url (cer/record-url (:es-conn es-conn)
                                   index-name
                                   index-type
                                   (:id zmap))]
    (cer/put (:es-conn es-conn)
             record-url
             {:body updated-record})
    (assoc updated-record
           :id (:id zmap))))
