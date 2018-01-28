(ns restro-search-engine.coercion.schema
  (:require [schema.core :as s]))


(defn non-empty-string?
  [string]
  (seq string))


(let [email-regex #"(?i)^[\p{L}\p{N}\p{M}\p{S}\p{Po}A-Z0-9._%'-]+(\+.*)?@[\p{L}\p{M}\p{N}\p{S}A-Z0-9'.-]+\.[\p{L}\p{M}\p{N}\p{S}A-Z]{2,4}"]
  (defn valid-email?
    "Validates the email-address. Throws exception if input is not a string."
    [email]
    (re-seq email-regex email)))


(defn valid-rating?
  [rating]
  (and (>= rating 1)
       (<= rating 5)))


(defn valid-ratings?
  [ratings]
  (let [valid-ratings (filter valid-rating?
                              ratings)]
    (= (count ratings) (count valid-ratings))))


(let [phone-number-regex #"^(\+\d{1,2})?[\s.-]?\(?\d{3}\)?[\s.-]?\d{3}[\s.-]?\d{4}$"]
  (defn valid-phone-number?
    [phone-number]
    (re-seq phone-number-regex phone-number)))


(let [location-regex #"^([-+]?\d{1,2}[.]\d+),\s*([-+]?\d{1,3}[.]\d+)$"]
  (defn valid-location?
    [location]
    (re-seq location-regex location)))

(s/defschema Ratings
  (s/constrained [s/Int] valid-ratings?))


(s/defschema MenuList
  {(s/required-key :dish_name) (s/constrained s/Str non-empty-string?)
   (s/required-key :veg) s/Bool
   (s/required-key :price) s/Int
   (s/required-key :category) (s/constrained s/Str non-empty-string?)
   (s/required-key :expected_preparation_duration) s/Int})


(s/defschema Restaurant
  {(s/required-key :title) (s/constrained s/Str non-empty-string?)
   (s/required-key :email) (s/constrained s/Str valid-email?)
   (s/required-key :phone_number) (s/constrained s/Str valid-phone-number?)
   (s/required-key :veg_only) s/Bool
   (s/required-key :delivery_only) s/Bool
   (s/required-key :expected_delivery_duration) s/Int
   (s/required-key :location) (s/constrained s/Str valid-location?)
   (s/optional-key :favourite_counts) s/Num
   (s/optional-key :ratings) Ratings
   (s/optional-key :menu_list) [MenuList]})


(s/defschema CreateDocumentRequest
  Restaurant)


(s/defschema RestaurantID
  (s/constrained s/Str non-empty-string?))


(s/defschema UpdateDocumentRequest
  {:id RestaurantID
   (s/optional-key :title) (s/constrained s/Str non-empty-string?)
   (s/optional-key :email) (s/constrained s/Str valid-email?)
   (s/optional-key :phone_number) (s/constrained s/Str valid-phone-number?)
   (s/optional-key :veg_only) s/Bool
   (s/optional-key :delivery_only) s/Bool
   (s/optional-key :expected_delivery_duration) s/Int
   (s/optional-key :location) (s/constrained s/Str valid-location?)
   (s/optional-key :favourite_counts) s/Num
   (s/optional-key :ratings) Ratings
   (s/optional-key :menu_list) [MenuList]})


(s/defschema GetRestaurantDocumentRequest
  {:id RestaurantID})


(s/defschema AddRatingsRequest
  {:id RestaurantID
   :rating (s/constrained s/Int valid-rating?)})


(s/defschema AddDishRequest
  (assoc MenuList
         :id RestaurantID))
