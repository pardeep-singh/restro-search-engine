(ns restro-search-engine.coercion.coerce
  (:require [schema.coerce :as scoerce]
            [restro-search-engine.coercion.schema :as rcs]))


(defn add-document-request-matchers
  [schema]
  (scoerce/json-coercion-matcher schema))


(defn update-document-request-matchers
  [schema]
  (scoerce/json-coercion-matcher schema))


(defn get-document-request-matchers
  [schema]
  (scoerce/json-coercion-matcher schema))


(defn add-rating-request-matchers
  [schema]
  (scoerce/json-coercion-matcher schema))


(defn add-dish-request-matchers
  [schema]
  (scoerce/json-coercion-matcher schema))


(def coerce-add-document-request
  (scoerce/coercer! rcs/CreateDocumentRequest
                    add-document-request-matchers))


(def coerce-update-document-request
  (scoerce/coercer! rcs/UpdateDocumentRequest
                    update-document-request-matchers))


(def coerce-get-document-request
  (scoerce/coercer! rcs/GetRestaurantDocumentRequest
                    get-document-request-matchers))


(def coerce-add-rating-request
  (scoerce/coercer! rcs/AddRatingsRequest
                    add-rating-request-matchers))


(def coerce-add-dish-request
  (scoerce/coercer! rcs/AddDishRequest
                    add-dish-request-matchers))
