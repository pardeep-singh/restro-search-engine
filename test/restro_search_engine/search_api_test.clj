(ns restro-search-engine.search-api-test
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
    (rmr/create-record (:elasticsearch system)
                       sample-restaurant-data)
    (rmr/create-record (:elasticsearch system)
                       (assoc sample-restaurant-data
                              :title "random"))
    (rmr/create-record (:elasticsearch system)
                       (assoc sample-restaurant-data
                              :title "Keventers"))
    (rmr/create-record (:elasticsearch system)
                       (assoc sample-restaurant-data
                              :title "McD"
                              :veg_only true
                              :delivery_only true
                              :favourite_counts 50
                              :ratings [5]
                              :expected_delivery_duration 50))
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


(deftest restaurants-search-query-test
  (testing "Search Restaurant by title"
    (let [result (http/post (str @service-url "/restaurants/search")
                            {:body (cc/generate-string {:query {:title "kfc"}})
                             :content-type :json
                             :throw-exceptions? false})
          body (cc/parse-string (:body result) true)]
      (is (= (:status result) 200)
          "Response status is 200")
      (is (= (->> (:hits body)
                  (mapv :title)
                  (filter #(clojure.string/includes? % "KFC"))
                  count)
             (count (:hits body)))
          "Response contains restaurants with KFC title only.")))
  (testing "Search Restaurant by veg_only field"
    (let [result (http/post (str @service-url "/restaurants/search")
                            {:body (cc/generate-string {:query {:veg_only true}})
                             :content-type :json
                             :throw-exceptions? false})
          body (cc/parse-string (:body result) true)]
      (is (= (:status result) 200)
          "Response status is 200")
      (is (= (->> (:hits body)
                  (mapv :veg_only)
                  (filter true?)
                  count)
             (count (:hits body)))
          "Response contains restaurants which are veg only.")))
  (testing "Search Restaurant by delivery_only field"
    (let [result (http/post (str @service-url "/restaurants/search")
                            {:body (cc/generate-string {:query {:delivery_only true}})
                             :content-type :json
                             :throw-exceptions? false})
          body (cc/parse-string (:body result) true)]
      (is (= (:status result) 200)
          "Response status is 200")
      (is (= (->> (:hits body)
                  (mapv :delivery_only)
                  (filter true?)
                  count)
             (count (:hits body)))
          "Response contains restaurants which provides delivery only.")))
  (testing "Search Restaurant by delivery_only field"
    (let [result (http/post (str @service-url "/restaurants/search")
                            {:body (cc/generate-string {:query {:favourite_counts {:greater_than_equal_to 0
                                                                                   :less_than_equal_to 200}}})
                             :content-type :json
                             :throw-exceptions? false})
          body (cc/parse-string (:body result) true)]
      (is (= (:status result) 200)
          "Response status is 200")
      (is (= (->> (:hits body)
                  (mapv :favourite_counts)
                  (filter #(and (>= % 0)
                                (<= % 200)))
                  count)
             (count (:hits body)))
          "Response contains restaurants which provides delivery only.")))
  (testing "Search Restaurant by expected_delivery_duration field"
    (let [result (http/post (str @service-url "/restaurants/search")
                            {:body (cc/generate-string {:query {:expected_delivery_duration {:greater_than_equal_to 0
                                                                                             :less_than_equal_to 200}}})
                             :content-type :json
                             :throw-exceptions? false})
          body (cc/parse-string (:body result) true)]
      (is (= (:status result) 200)
          "Response status is 200")
      (is (= (->> (:hits body)
                  (mapv :expected_delivery_duration)
                  (filter #(and (>= % 0)
                                (<= % 200)))
                  count)
             (count (:hits body)))
          "Response contains restaurants where expected_delivery_duration is in the given range")))
  (testing "Search Restaurant by expected_delivery_duration field"
    (let [result (http/post (str @service-url "/restaurants/search")
                            {:body (cc/generate-string {:query {:menu_list {:veg true
                                                                            :dish_name "Veg Zinger Burger"
                                                                            :category "Fast Food"}}})
                             :content-type :json
                             :throw-exceptions? false})
          body (cc/parse-string (:body result) true)
          menu_list (->> (:hits body)
                         first
                         :menu_list
                         (mapv #(select-keys % [:veg :dish_name :category]))
                         (some #{{:veg true :dish_name "Veg Zinger Burger" :category "Fast Food"}}))]
      (is (= (:status result) 200)
          "Response status is 200")
      (is (seq menu_list)
          "Response contains restaurant with given menu_list query."))))


(deftest restaurants-search-fields-test
  (testing "Response only includes given fields."
    (let [result (http/post (str @service-url "/restaurants/search")
                            {:body (cc/generate-string {:fields ["favourite_counts" "veg_only"]})
                             :content-type :json
                             :throw-exceptions? false})
          body (cc/parse-string (:body result) true)]
      (is (= (:status result) 200)
          "Response status is 200")
      (is (= (count (:hits body))
             (->> (:hits body)
                  (mapv keys)
                  (filter #(= [:veg_only :favourite_counts :id] %))
                  count))
          "Response contains only given fields + id."))))


(deftest restaurants-search-api-sorting-test
  (testing "Result is sorted by favourite_counts in desc order"
    (let [result (http/post (str @service-url "/restaurants/search")
                            {:body (cc/generate-string {:sort-field "favourite_counts"
                                                        :sort-order "desc"})
                             :content-type :json
                             :throw-exceptions? false})
          body (cc/parse-string (:body result) true)]
      (is (= (:status result) 200)
          "Response status is 200")
      (is (= (mapv :favourite_counts (:hits body))
             (sort > (mapv :favourite_counts (:hits body))))
          "Result is sorted by desc order.")))
  (testing "Result is sorted by favourite_counts in desc order"
    (let [result (http/post (str @service-url "/restaurants/search")
                            {:body (cc/generate-string {:sort-field "favourite_counts"
                                                        :sort-order "asc"})
                             :content-type :json
                             :throw-exceptions? false})
          body (cc/parse-string (:body result) true)]
      (is (= (:status result) 200)
          "Response status is 200")
      (is (= (mapv :favourite_counts (:hits body))
             (sort (mapv :favourite_counts (:hits body))))
          "Result is sorted by asc order"))))


(deftest restaurants-search-api-pagination-test
  (testing "Result is sorted by favourite_counts in desc order"
    (let [result (http/post (str @service-url "/restaurants/search")
                            {:body (cc/generate-string {:page 1
                                                        :page-size 1})
                             :content-type :json
                             :throw-exceptions? false})
          body (cc/parse-string (:body result) true)]
      (is (= (:status result) 200)
          "Response status is 200")
      (is (= (:total_hits body) (:total_pages body))
          "Total hits are equal to total pages as page-size is 1."))))


(deftest restaurants-search-api-aggregations-test
  (testing "Result is includes aggregations for given fields"
    (let [result (http/post (str @service-url "/restaurants/search")
                            {:body (cc/generate-string {:aggs ["veg_only" "delivery_only" "price" "ratings"]})
                             :content-type :json
                             :throw-exceptions? false})
          body (cc/parse-string (:body result) true)]
      (is (= (:status result) 200)
          "Response status is 200")
      (is (= (keys (:aggregations body))
             [:veg_only :menu_list :delivery_only :ratings])
          "Response includes aggregations for given fields."))))
