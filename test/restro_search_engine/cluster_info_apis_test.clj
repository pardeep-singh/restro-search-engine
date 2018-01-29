(ns restro-search-engine.cluster-info-apis-test
  (:require  [clojure.test :refer :all]
             [cheshire.core :as cc]
             [clj-http.client :as http]
             [restro-search-engine.core :as rc]
             [restro-search-engine.util.util :as ruu]))


(def service-port (atom nil))
(def server-system (atom nil))
(def service-url (atom nil))


(defn test-setup
  []
  (let [configs (ruu/read-configs)
        random-service-port (+ (:port configs)
                               (rand-int 1000))
        system (-> configs
                   (assoc :port random-service-port)
                   rc/construct-system
                   rc/start-system)]
    (reset! service-port random-service-port)
    (reset! server-system system)
    (reset! service-url (str "http://localhost:" @service-port))))


(defn test-cleanup
  []
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


(deftest get-cluster-info-api-tests
  (testing "/cluster api contains expected values"
    (let [result (http/get (str @service-url "/cluster"))
          body (cc/parse-string (:body result) true)]
      (is (= (:status result)
             200)
          "Response status is 200.")
      (is (= (keys body)
             [:name :cluster_name :cluster_uuid :version :tagline])
          "Response contains expected keys."))))


(deftest get-cluster-health-api-tests
  (testing "/cluster/_health api contains expected values"
    (let [result (http/get (str @service-url "/cluster/_health"))
          body (cc/parse-string (:body result) true)]
      (is (= (:status result)
             200)
          "Response status is 200.")
      (is (= (keys body)
             [:active_shards :task_max_waiting_in_queue_millis :active_shards_percent_as_number
              :number_of_nodes :unassigned_shards :delayed_unassigned_shards :timed_out :status
              :relocating_shards :cluster_name :number_of_in_flight_fetch :number_of_pending_tasks
              :initializing_shards :number_of_data_nodes :active_primary_shards])
          "Response contains expected keys."))))


