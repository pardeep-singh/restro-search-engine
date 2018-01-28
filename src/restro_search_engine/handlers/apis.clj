(ns restro-search-engine.handlers.apis
  (:require [restro-search-engine.models.cluster :as rmc]
            [restro-search-engine.models.restaurants :as rmr]
            [restro-search-engine.coercion.coerce :as rcc]))


(defn get-cluster-info
  [elasticsearch]
  (rmc/cluster-info elasticsearch))


(defn get-cluster-health
  [elasticsearch]
  (rmc/cluster-health elasticsearch))


(defn get-restaurant-record
  [elasticseach record]
  (->> record
       rcc/coerce-get-document-request
       (rmr/fetch-restaurant-record elasticseach)))


(defn create-restaurant-record
  [elasticsearch record]
  (->> record
       rcc/coerce-add-document-request
       (rmr/create-record elasticsearch)))


(defn update-restaurant-record
  [elasticsearch record]
  (->> record
       rcc/coerce-update-document-request
       (rmr/update-record elasticsearch)))


(defn search-restaurant-record
  [elasticsearch search-query]
  (->> search-query
       rcc/coerce-search-request
       (rmr/search-restaurants elasticsearch)))


(defn add-ratings
  [elasticsearch zmap]
  (->> zmap
       rcc/coerce-add-rating-request
       (rmr/add-ratings elasticsearch)))


(defn add-dish
  [elasticsearch zmap]
  (->> zmap
       rcc/coerce-add-dish-request
       (rmr/add-dish elasticsearch)))


(defn suggestions
  [elasticsearch query]
  (->> query
       rcc/coerce-get-suggestion-request
       (rmr/autocompletion elasticsearch)))
