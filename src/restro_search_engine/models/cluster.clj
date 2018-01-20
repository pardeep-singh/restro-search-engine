(ns restro-search-engine.models.cluster
  (:require [clojurewerkz.elastisch.rest :as cer]))


(defn cluster-info
  [{:keys [es-conn]}]
  (->> (cer/url-with-path es-conn)
       (cer/get es-conn)))

(defn cluster-health
  [{:keys [es-conn]}]
  (->> (cer/cluster-health-url es-conn nil)
       (cer/get es-conn)))
