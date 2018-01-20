(ns restro-search-engine.handlers.apis
  (:require [restro-search-engine.models.cluster :as rmc]))


(defn get-cluster-info
  [elasticsearch]
  (rmc/cluster-info elasticsearch))


(defn get-cluster-health
  [elasticsearch]
  (rmc/cluster-health elasticsearch))
