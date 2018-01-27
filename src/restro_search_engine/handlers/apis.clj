(ns restro-search-engine.handlers.apis
  (:require [restro-search-engine.models.cluster :as rmc]
            [restro-search-engine.models.restaurants :as rmr]))


(defn get-cluster-info
  [elasticsearch]
  (rmc/cluster-info elasticsearch))


(defn get-cluster-health
  [elasticsearch]
  (rmc/cluster-health elasticsearch))


(defn get-restaurant-record
  [elasticseach record-id]
  (rmr/fetch-restaurant-record elasticseach record-id))


(defn create-restaurant-record
  [elasticsearch record]
  (rmr/create-record elasticsearch record))


(defn update-restaurant-record
  [elasticsearch record]
  (rmr/update-record elasticsearch record))


(defn search-restaurant-record
  [elasticsearch search-query]
  (rmr/search-restaurants elasticsearch search-query))


(defn add-ratings
  [elasticsearch zmap]
  (rmr/add-ratings elasticsearch zmap))


(defn add-dish
  [elasticsearch zmap]
  (rmr/add-dish elasticsearch zmap))


(defn suggestions
  [elasticsearch query]
  (rmr/autocompletion elasticsearch query))
