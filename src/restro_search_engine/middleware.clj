(ns restro-search-engine.middleware
  (:require [clojure.tools.logging :as ctl]
            [restro-search-engine.util.http :as ruh]
            [slingshot.slingshot :refer [try+]]))


(defn wrap-exceptions
  [handler]
  (fn [req]
    (try+
     (handler req)
     (catch restro_search_engine.util.http.HTTPError http-exception
       (ctl/error http-exception
                  "HTTP Exception")
       (ruh/http-error http-exception))
     (catch Exception exception
       (ctl/error exception
                  "Internal Server Error")
       (ruh/internal-server-error "Internal Server Error")))))


(defn log-requests
  [handler]
  (fn [req]
    (let [timestamp (System/currentTimeMillis)
          response (handler req)]
      (ctl/info {:status (:status response)
                 :method (:request-method req)
                 :uri (:uri req)
                 :duration (str (- (System/currentTimeMillis)
                                   timestamp)
                                "ms")})
      response)))
