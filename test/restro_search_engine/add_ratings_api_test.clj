(ns restro-search-engine.add-ratings-api-test
  (:require  [clojure.test :refer :all]
             [cheshire.core :as cc]
             [clj-http.client :as http]
             [restro-search-engine.core :as rc]
             [restro-search-engine.util.util :as ruu]
             [restro-search-engine.models.restaurants :as rmr]
             [bond.james :refer [with-stub]]
             [clojure.tools.logging :as ctl]))


(def service-port (atom nil))
(def server-system (atom nil))
(def service-url (atom nil))
(def index-name (atom nil))
(def doc-id (atom nil))
(def sample-restaurant-data {:title "KFC"
                             :email "kfc@mail.com"
                             :phone_number "+919955599555"
                             :favourite_counts 100
                             :veg_only false
                             :delivery_only false
                             :location "18.1,74.1"
                             :expected_delivery_duration 100
                             :ratings [1 2 3]
                             :menu_list [{:dish_name "Chicken Popcorns"
                                          :veg false
                                          :price 100
                                          :category "Quickbite"
                                          :expected_preparation_duration 5}
                                         {:dish_name "Veg Zinger Burger"
                                          :veg true
                                          :price 150
                                          :category "Fast Food"
                                          :expected_preparation_duration 10}]})

(defn test-setup
  []
  (let [configs (ruu/read-configs)
        random-service-port (+ (:port configs)
                               (rand-int 1000))
        system (-> configs
                   (assoc :port random-service-port)
                   rc/construct-system
                   rc/start-system)
        random-index-name (str rmr/index-name
                               (apply str (take 5 (repeatedly #(char (+ (rand 26) 97))))))]
    
    (ruu/create-index* (:elasticsearch system)
                       random-index-name
                       rmr/index-settings-mappings)
    (alter-var-root #'rmr/index-name (constantly random-index-name))
    (reset! doc-id (:id (rmr/create-record (:elasticsearch system)
                                           sample-restaurant-data)))
    (reset! index-name random-index-name)
    (reset! service-port random-service-port)
    (reset! server-system system)
    (reset! service-url (str "http://localhost:" @service-port))))


(defn test-cleanup
  []
  (ruu/delete-index (:elasticsearch @server-system)
                    @index-name)
  (rc/stop-system @server-system)
  (reset! server-system nil))


(defn once-fixture
  [tests]
  (test-setup)
  (tests)
  (test-cleanup))


(defn each-fixture
  [tests]
  (tests))


(use-fixtures :once once-fixture)
(use-fixtures :each each-fixture)


(deftest add-restaurant-ratings-api-test
  (testing "Add Restaurant Ratings API test"
    (let [result (http/post (str @service-url "/restaurants/" @doc-id "/ratings")
                            {:body (cc/generate-string {:rating 4})
                             :content-type :json
                             :throw-exceptions? false})
          body (cc/parse-string (:body result) true)]
      (is (= (:status result) 200)
          "Response status is 200")
      (is (= (:ratings body)
             [1 2 3 4])
          "Response contains updated value of ratings field."))))


(deftest add-restaurant-rating-api-test-with-invalid-values
  (testing "Add Restaurant Rating API test with invalid id"
    (with-stub [[ctl/log* (fn [& args] (constantly true))]]
      (let [result (http/post (str @service-url "/restaurants/random/ratings")
                              {:body (cc/generate-string {:rating 1})
                               :content-type :json
                               :throw-exceptions? false})
            body (cc/parse-string (:body result) true)]
        (is (= (:status result) 404)
            "Response status is 404"))))
  (testing "Add Restaurant Rating API test with invalid values"
    (with-stub [[ctl/log* (fn [& args] (constantly true))]]
      (let [result (http/post (str @service-url "/restaurants/" @doc-id "/ratings")
                              {:body (cc/generate-string {:rating 100})
                               :content-type :json
                               :throw-exceptions? false})
            body (cc/parse-string (:body result) true)]
        (is (= (:status result) 400)
            "Response status is 400")
        (is (= (:invalid-fields body)
               ["rating"])
            "Response contains invalid-fields with rating field.")))))
