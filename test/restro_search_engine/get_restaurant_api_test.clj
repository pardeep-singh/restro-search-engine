(ns restro-search-engine.get-restaurant-api-test
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
(def sample-doc {:title "KFC"
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
                                           sample-doc)))
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


(deftest fetch-document-for-given-doc-id
  (testing "Get Document given a doc id"
    (let [result (http/get (str @service-url "/restaurants/" @doc-id))
          body (cc/parse-string (:body result) true)]
      (is (= (:status result) 200)
          "Response status is 200.")
      (is (= sample-doc body)
          "Response contains created document with doc id."))))


(deftest fetch-document-with-invalid-id
  (with-stub [[ctl/log* (fn [& args] (constantly true))]]
    (testing "Get Document given a doc id"
      (let [result (http/get (str @service-url "/restaurants/random")
                             {:throw-exceptions? false})]
        (is (= (:status result) 404)
            "Response status is 404.")))))
