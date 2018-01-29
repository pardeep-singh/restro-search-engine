(ns restro-search-engine.util.util
  (:require [restro-search-engine.models.restaurants :as rmr]
            [restro-search-engine.components :as rc]
            [clj-http.client :as http]
            [cheshire.core :as cc]
            [com.stuartsierra.component :as csc]
            [clojure.java.io :refer [resource]]
            [clojure.edn :as edn]))


(defonce default-settings-file "configs.clj")


(defn create-index*
  [{:keys [es-conn]} index-name index-configs]
  (let [uri (str (:uri es-conn) "/" index-name)
        result (http/put uri
                         (merge (:http-opts es-conn)
                                {:body (cheshire.core/generate-string index-configs)
                                 :accept :json}))]
    result))


(defn create-index
  [& {:keys [index index-configs]
      :or {index rmr/index-name
           index-configs rmr/index-settings-mappings}}]
  (let [es-comp (rc/map->Elasticsearch {:host "192.168.33.21"
                                        :port "9200"})
        system (csc/start es-comp)
        response (create-index* system
                                index
                                index-configs)]
    (cc/parse-string (:body response)
                     true)))


(defn read-configs
  ([]
   (read-configs default-settings-file))
  ([file-name]
   (let [file-path ((comp str resource) default-settings-file)]
     (edn/read-string (slurp file-path)))))
