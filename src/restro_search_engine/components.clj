(ns restro-search-engine.components
  (:require [ring.adapter.jetty :refer [run-jetty]]
            [com.stuartsierra.component :as csc]
            [clojurewerkz.elastisch.rest :as  cer]
            [clj-http.conn-mgr :refer [make-reusable-conn-manager]]))


;; Component to setup the APP routes
(defrecord Routes [app elasticsearch]
  csc/Lifecycle

  (start [this]
    (println "setting up routes components.")
    (assoc this
           :routes (app elasticsearch)))

  (stop [this]
    (println "stopping routes")
    (assoc this
           :routes nil)))


;; Component to manage the Jetty Server Lifecycle
(defrecord HttpServer [port routes]
  csc/Lifecycle

  (start [this]
    (let [server (run-jetty (:routes routes)
                            {:port port
                             :join? false})]
      (println "started jetty server")
      (assoc this
             :http-server server)))

  (stop [this]
    (println "stopping jetty server")
    (.stop ^org.eclipse.jetty.server.Server (:http-server this))
    (assoc this
           :http-server nil)))


(defrecord Elasticsearch [host port]
  csc/Lifecycle

  (start [this]
    (let [es-url (format "http://%s:%s" host port)
          conn (cer/connect es-url
                            {:connection-manager (make-reusable-conn-manager {:timeout 10})})]
      (println es-url this)
      (assoc this
             :es-conn conn)))

  (stop [this]
    (assoc this
           :es-conn nil)))
