(ns restro-search-engine.middlware
  (:require [clojure.tools.logging :as ctl]
            [restro-search-engine.util.http :as ruh]))


(defn wrap-exceptions
  [handler]
  (fn [req]
    (try
      (handler req)
      (catch Exception exception
        (ctl/error exception
                   "Internal Server Error")
        (ruh/internal-server-error "Internal Server Error")))))

