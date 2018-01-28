(ns restro-search-engine.coercion.coerce
  (:require [schema.coerce :as scoerce]
            [restro-search-engine.coercion.schema :as rcs]))


(defn add-document-request-matchers
  [schema]
  (scoerce/json-coercion-matcher schema))


(def coerce-add-document-request
  (scoerce/coercer! rcs/CreateDocumentRequest
                    add-document-request-matchers))

