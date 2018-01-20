(ns restro-search-engine.core
  (:gen-class)
  (:require [compojure.core :as cc :refer [context defroutes POST GET PUT DELETE]]
            [compojure.route :as route]
            [ring.middleware
             [keyword-params :refer [wrap-keyword-params]]
             [params :refer [wrap-params]]]
            [com.stuartsierra.component :as csc]
            [restro-search-engine.components :as rc]
            [clojure.tools.logging :as ctl]
            [restro-search-engine.util.http :as ruh]
            [restro-search-engine.handlers.apis :as rha]
            [restro-search-engine.middlware :as rm]))


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

   (route/not-found "Not Found")))


(defn app
  "Constructs routes wrapped by middlewares."
  [elasticsearch]
  (-> (app-routes elasticsearch)
      wrap-keyword-params
      wrap-params
      rm/wrap-exceptions))


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


(defn -main
  [& args]
  (try
    (let [es-comp (rc/map->Elasticsearch {:host "192.168.33.21"
                                          :port "9200"})
          routes-comp (rc/map->Routes {:app app})
          http-server-comp (rc/map->HttpServer {:port 7799})
          system (csc/system-map
                  :elasticsearch es-comp
                  :routes (csc/using routes-comp
                                     [:elasticsearch])
                  :http-server (csc/using http-server-comp
                                          [:routes]))]
      (start-system system)
      (.addShutdownHook (Runtime/getRuntime)
                        (Thread. (fn []
                                   (ctl/info "Running Shutdown Hook")
                                   (stop-system server-system)
                                   (shutdown-agents)
                                   (ctl/info "Done with shutdown hook")))))
    (catch Exception exception
      (ctl/error exception
                 "Exception while starting the application"))))
