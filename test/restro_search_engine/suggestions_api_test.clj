(ns restro-search-engine.suggestions-api-test
  (:require  [clojure.test :refer :all]
             [cheshire.core :as cc]
             [clj-http.client :as http]
             [restro-search-engine.core :as rc]
             [restro-search-engine.util.util :as ruu]
             [restro-search-engine.models.restaurants :as rmr]
             [bond.james :refer [with-stub]]
             [clojure.tools.logging :as ctl]
             [clojurewerkz.elastisch.rest.index :as ceri]))

(def service-port (atom nil))
(def server-system (atom nil))
(def service-url (atom nil))
(def index-name (atom nil))
(def doc-ids (atom []))
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
    (reset! doc-ids (conj @doc-ids
                          (:id (rmr/create-record (:elasticsearch system)
                                                  sample-restaurant-data))))
    (rmr/create-record (:elasticsearch system)
                       (assoc sample-restaurant-data
                              :title "random"))
    (reset! doc-ids (conj @doc-ids
                          (:id (rmr/create-record (:elasticsearch system)
                                                  (assoc sample-restaurant-data
                                                         :title "Keventers")))))
    (ceri/refresh (:es-conn (:elasticsearch system)))
    (Thread/sleep 5000)
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


(deftest restaurants-suggestions-api-test
  (testing "Restaurant Suggestions API test"
    (let [result (http/post (str @service-url "/restaurants/suggest")
                            {:body (cc/generate-string {:title "k"})
                             :content-type :json
                             :throw-exceptions? false})
          body (cc/parse-string (:body result) true)]
      (is (= (:status result) 200)
          "Response status is 200")
      (is (= 2 (->> (:suggestions body)
                    (mapv :title)
                    (filter #(clojure.string/starts-with? % "K"))
                    count))
          "Response contains restaurants which starts with K only."))))
