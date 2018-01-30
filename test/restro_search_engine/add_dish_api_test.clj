(ns restro-search-engine.add-dish-api-test
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


(deftest add-dish-api-test
  (testing "Add Dish to Restaurant Menulist API test"
    (let [dish-object {:dish_name "Chicken Wings"
                       :price 105
                       :veg false
                       :expected_preparation_duration 15
                       :category "Quick Bite"}
          result (http/post (str @service-url "/restaurants/" @doc-id "/menulist")
                            {:body (cc/generate-string dish-object)
                             :content-type :json
                             :throw-exceptions? false})
          body (cc/parse-string (:body result) true)]
      (println (some #{dish-object}
                     (:menu_list body)))
      (is (= (:status result) 200)
          "Response status is 200"))))


(deftest add-dish-api-test-with-invalid-values
  (testing "Add Dish to Restaurant Menulist API test with invalid id"
    (with-stub [[ctl/log* (fn [& args] (constantly true))]]
      (let [result (http/post (str @service-url "/restaurants/random/menulist")
                              {:body (cc/generate-string {:dish_name "Chicken Wings"
                                                          :price 105
                                                          :veg false
                                                          :expected_preparation_duration 15
                                                          :category "Quick Bite"})
                               :content-type :json
                               :throw-exceptions? false})
            body (cc/parse-string (:body result) true)]
        (is (= (:status result) 404)
            "Response status is 404"))))
  (testing "Add Dish to Restaurant Menulist API test with invalid values"
    (with-stub [[ctl/log* (fn [& args] (constantly true))]]
      (let [result (http/post (str @service-url "/restaurants/" @doc-id "/menulist")
                              {:body (cc/generate-string {:dish_name "Chicken Wings"
                                                          :price "RS105"
                                                          :veg 1
                                                          :expected_preparation_duration 15
                                                          :category "Quick Bite"})
                               :content-type :json
                               :throw-exceptions? false})
            body (cc/parse-string (:body result) true)]
        (is (= (:status result) 400)
            "Response status is 400")
        (is (= (:invalid-fields body)
               ["veg" "price"])
            "Response contains invalid-fields with price, veg fields.")))))
