(ns restro-search-engine.components
  (:require [ring.adapter.jetty :refer [run-jetty]]
            [com.stuartsierra.component :as csc]))


;; Component to setup the APP routes
(defrecord Routes [app]
  csc/Lifecycle

  (start [this]
    (println "setting up routes components.")
    (assoc this
           :routes (app)))

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
