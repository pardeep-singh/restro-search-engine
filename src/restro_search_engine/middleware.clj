(ns restro-search-engine.middleware
  (:require [clojure.tools.logging :as ctl]
            [restro-search-engine.util.http :as ruh]
            [slingshot.slingshot :refer [try+]]))


(defn parse-invalid-schema-exception
  "Parses invalid schema exception."
  [error]
  (let [errors (seq (:error (ex-data error)))
        grouped-errors (group-by (fn [[k v]]
                                   (if (= v 'missing-required-key)
                                     :missing-required-key
                                     :validation-error))
                                 errors)
        missing-keys (map first (:missing-required-key grouped-errors))
        validation-errors (map first (:validation-error grouped-errors))
        validation-map {:missing-fields missing-keys
                        :invalid-fields validation-errors}]
    validation-map))


(defn wrap-exceptions
  [handler]
  (fn [req]
    (try+
     (handler req)
     (catch clojure.lang.ExceptionInfo ei
       (let [validation-error (parse-invalid-schema-exception ei)]
         (ctl/warn ei
                   "Validation Error")
         (ruh/bad-request validation-error)))
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
