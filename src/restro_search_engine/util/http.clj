(ns restro-search-engine.util.http
  (:require [cheshire.core :as cc]))


(defrecord HTTPError [type status msg data])


(defn json-response
  [response & {:keys [status]}]
  {:status status
   :headers {"Content-Type" "application/json"}
   :body (when response
           (cc/generate-string response))})


(defn ok
  [zmap]
  (json-response zmap
                 :status 200))


(defn ok-no-content
  [zmap]
  (json-response zmap
                 :status 204))

(defn created
  [zmap]
  (json-response zmap
                 :status 201))


(defn internal-server-error
  [message]
  (json-response {:error message}
                 :status 500))


(defn not-found
  [message]
  (json-response {:error message}
                 :status 404))


(defn not-found-exception
  [msg]
  (HTTPError. ::not-found
              404
              msg
              nil))


(defn http-error
  [{:keys [status] :as response}]
  (json-response response
                 :status status))


(defn bad-request
  [zmap]
  (json-response zmap
                 :status 400))
