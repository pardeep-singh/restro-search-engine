(ns restro-search-engine.core
  (:gen-class)
  (:require [compojure.core :as cc :refer [context defroutes POST GET PUT DELETE]]
            [compojure.route :as route]
            [ring.middleware
             [keyword-params :refer [wrap-keyword-params]]
             [params :refer [wrap-params]]]
            [ring.middleware.json :refer [wrap-json-params]]
            [com.stuartsierra.component :as csc]
            [restro-search-engine.components :as rc]
            [clojure.tools.logging :as ctl]
            [restro-search-engine.util.http :as ruh]
            [restro-search-engine.handlers.apis :as rha]
            [restro-search-engine.middleware :as rm]
            [restro-search-engine.util.util :as ruu]
            [clojurewerkz.elastisch.rest.index :as ceri]
            [restro-search-engine.models.restaurants :as rmr]))


(defonce ^{:doc "Server system representing HTTP server."}
  server-system nil)


(defn app-routes
  "Returns the APP routes and injects the dependency required by routes."
  [elasticsearch]
  (cc/routes
   (GET "/ping" [] (ruh/ok {:ping "PONG"}))

   (context "/cluster" []
            (GET "/" []
                 (ruh/ok (rha/get-cluster-info elasticsearch)))

            (GET "/_health" []
                 (ruh/ok (rha/get-cluster-health elasticsearch))))

   (context "/restaurants" []
            (GET "/:id" {m :params}
                 (ruh/ok (rha/get-restaurant-record elasticsearch m)))
            (POST "/" {m :params}
                  (ruh/created (rha/create-restaurant-record elasticsearch m)))
            (PUT "/:id" {m :params}
                 (ruh/ok (rha/update-restaurant-record elasticsearch m)))
            (POST "/search" {m :params}
                  (ruh/ok (rha/search-restaurant-record elasticsearch m)))
            (POST "/:id/ratings" {m :params}
                  (ruh/ok (rha/add-ratings elasticsearch m)))
            (POST "/:id/menulist" {m :params}
                  (ruh/ok (rha/add-dish elasticsearch m)))
            (POST "/suggest" {m :params}
                  (ruh/ok (rha/suggestions elasticsearch m))))

   (route/not-found "Not Found")))


(defn app
  "Constructs routes wrapped by middlewares."
  [elasticsearch]
  (-> (app-routes elasticsearch)
      wrap-keyword-params
      wrap-params
      wrap-json-params
      rm/wrap-exceptions
      rm/log-requests))


(defn start-system
  "Starts the system given system-map."
  [system]
  (let [server-system* (csc/start system)]
    (alter-var-root #'server-system (constantly server-system*))))


(defn stop-system
  "Stops the system given a system-map."
  [system]
  (let [server-system* (csc/stop system)]
    (alter-var-root #'server-system (constantly server-system*))))


(defn construct-system
  [configs]
  (let [es-comp (rc/map->Elasticsearch (:elasticsearch configs))
        routes-comp (rc/map->Routes {:app app})
        http-server-comp (rc/map->HttpServer {:port (:port configs)})]
    (csc/system-map
     :elasticsearch es-comp
     :routes (csc/using routes-comp
                        [:elasticsearch])
     :http-server (csc/using http-server-comp
                             [:routes]))))


(defn -main
  [& args]
  (try
    (let [configs (ruu/read-configs)
          system (construct-system configs)]
      (start-system system)
      (when-not (ceri/exists? (get-in server-system [:elasticsearch :es-conn])
                              rmr/index-name)
        (ctl/info (format "Creating Index: %s" rmr/index-name))
        (ruu/create-index* (:elasticsearch server-system)
                           rmr/index-name
                           rmr/index-settings-mappings))
      (.addShutdownHook (Runtime/getRuntime)
                        (Thread. (fn []
                                   (ctl/info "Running Shutdown Hook")
                                   (stop-system server-system)
                                   (shutdown-agents)
                                   (ctl/info "Done with shutdown hook")))))
    (catch Exception exception
      (ctl/error exception
                 "Exception while starting the application"))))
