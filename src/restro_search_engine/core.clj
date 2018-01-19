(ns restro-search-engine.core
  (:gen-class)
  (:require [compojure.core :as cc :refer [context defroutes POST GET PUT DELETE]]
            [compojure.route :as route]
            [ring.middleware
             [keyword-params :refer [wrap-keyword-params]]
             [params :refer [wrap-params]]]
            [com.stuartsierra.component :as csc]
            [restro-search-engine.components :as rc]))


(defonce ^{:doc "Server system representing HTTP server."}
  server-system nil)


(defn app-routes
  "Returns the APP routes and injects the dependency required by routes."
  []
  (cc/defroutes routes
    (GET "/ping" [] "pong")

    (route/not-found "Not Found")))


(defn app
  "Constructs routes wrapped by middlewares."
  []
  (-> (app-routes)
      wrap-keyword-params
      wrap-params))


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
  (let [routes-comp (rc/map->Routes {:app app})
        http-server-comp (rc/map->HttpServer {:port 7799})
        system (csc/system-map
                :routes routes-comp
                :http-server (csc/using http-server-comp
                                        [:routes]))]
    (start-system system)
    (.addShutdownHook (Runtime/getRuntime)
                      (Thread. (fn []
                                 (println "Running Shutdown Hook")
                                 (stop-system server-system)
                                 (shutdown-agents)
                                 (println "Done with shutdown hook"))))))
