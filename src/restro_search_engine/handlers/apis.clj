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
