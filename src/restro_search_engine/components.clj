(ns restro-search-engine.components
  (:require [ring.adapter.jetty :refer [run-jetty]]
            [com.stuartsierra.component :as csc]
            [clojurewerkz.elastisch.rest :as  cer]
            [clj-http.conn-mgr :refer [make-reusable-conn-manager]]
            [clojure.tools.logging :as ctl]))


;; Component to setup the APP routes
(defrecord Routes [app elasticsearch routes]
  csc/Lifecycle

  (start [this]
    (if (nil? routes)
      (do
        (ctl/info "Setting up routes component")
        (assoc this
               :routes (app elasticsearch)))
      (do
        (ctl/info "Routes component already started")
        this)))

  (stop [this]
    (if routes
      (do
        (ctl/info "Removing routes component")
        (assoc this
               :routes nil))
      (do
        (ctl/info "Routes component is nil")
        this))))


;; Component to manage the Jetty Server Lifecycle
(defrecord HttpServer [port routes http-server]
  csc/Lifecycle

  (start [this]
    (if (nil? http-server)
      (let [server (run-jetty (:routes routes)
                              {:port port
                               :join? false})]
        (ctl/info "Starting HTTP server")
        (assoc this
               :http-server server))
      (do
        (ctl/info "HTTP server already started")
        this)))

  (stop [this]
    (if http-server
      (do
        (ctl/info "Stopping HTTP server")
        (.stop ^org.eclipse.jetty.server.Server (:http-server this))
        (assoc this
               :http-server nil))
      (do
        (ctl/info "HTTP server component is nil")
        this))))


(defrecord Elasticsearch [host port es-conn]
  csc/Lifecycle

  (start [this]
    (if (nil? es-conn)
      (do
        (ctl/info "Starting ES component")
        (let [es-url (format "http://%s:%s" host port)
              conn (cer/connect es-url
                                {:connection-manager (make-reusable-conn-manager {:timeout 10})})]
          (assoc this
                 :es-conn conn)))
      (do
        (ctl/info "ES component already started")
        this)))

  (stop [this]
    (if es-conn
      (do
        (ctl/info "Stopping ES component")
        (assoc this
               :es-conn nil))
      (do
        (ctl/info "ES component is nil")
        this))))
