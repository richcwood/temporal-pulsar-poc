(ns com.bamboohr.event-publisher
  (:require [com.bamboohr.flow-logger :as log])
  (:gen-class)
  (:import [org.apache.pulsar.client.api PulsarClient Schema Producer ProducerBuilder]
           [java.util Map]))

;; Configuration
(def ^:private default-pulsar-url "pulsar://localhost:6650")
(def ^:private default-topic "persistent://public/default/event-handler")
(def ^:private default-num-events 5)

;; Delay configuration
(def ^:private min-delay-ms 10)
(def ^:private max-delay-ms 80)

;; Event types configuration
(def ^:private event-types
  [{:entity-type "Employee"
    :operation "Hire"
    :payload-fn #(hash-map
                  "name" (str "Employee " %)
                  "department" (rand-nth ["Engineering" "Sales" "Marketing" "HR"])
                  "hire_date" (System/currentTimeMillis))}
   {:entity-type "Employee"
    :operation "Termination"
    :payload-fn #(hash-map
                  "name" (str "Employee " %)
                  "termination_date" (System/currentTimeMillis)
                  "reason" (rand-nth ["Voluntary" "Involuntary"]))}
   {:entity-type "TimeOff"
    :operation "Request"
    :payload-fn #(hash-map
                  "employee_id" %
                  "start_date" (System/currentTimeMillis)
                  "days" (inc (rand-int 14))
                  "type" (rand-nth ["Vacation" "Sick" "Personal"]))}
   {:entity-type "Payroll"
    :operation "Run"
    :payload-fn #(hash-map
                  "payroll_id" %
                  "period_start" (- (System/currentTimeMillis) (* 14 24 60 60 1000))
                  "period_end" (System/currentTimeMillis)
                  "status" "Completed")}
   {:entity-type "Department"
    :operation "Update"
    :payload-fn #(hash-map
                  "department_id" %
                  "name" (rand-nth ["Engineering" "Sales" "Marketing" "HR"])
                  "manager_id" (rand-int 1000))}])

;; Client management
(defn create-client
  "Creates a Pulsar client with the given service URL"
  [service-url]
  (-> (PulsarClient/builder)
      (.serviceUrl service-url)
      .build))

(defn create-producer
  "Creates a producer for the given client and topic"
  [client topic]
  (-> client
      (.newProducer (Schema/JSON Map))
      (.topic topic)
      .create))

;; Event generation
(defn generate-event
  "Generates a single random HRIS event with the given ID"
  [id]
  (let [event-type (rand-nth event-types)]
    {"entity-id" id
     "entity-type" (:entity-type event-type)
     "operation" (:operation event-type)
     "timestamp" (System/currentTimeMillis)
     "payload" ((:payload-fn event-type) id)}))

(defn random-delay
  "Generates a random delay between min-delay-ms and max-delay-ms"
  []
  (+ min-delay-ms (rand-int (- max-delay-ms min-delay-ms))))

;; Publishing logic
(defn publish-event
  "Publishes a single event and logs the action"
  [producer event]
  (.send producer event)
  (log/log-flow :message (merge {:message_type "Published event"} event)))

(defn publish-events
  "Publishes multiple events with random delays between them"
  [producer num-events]
  (doseq [i (range num-events)]
    (let [event (generate-event i)]
      (publish-event producer event)
      (Thread/sleep (random-delay)))))

;; Resource management
(defn with-resources
  "Executes body with client and producer, ensuring they are properly closed"
  [client producer f]
  (try
    (f)
    (finally
      (.close producer)
      (.close client))))

;; Main entry point
(defn -main [& args]
  (let [num-events (try
                     (Integer/parseInt (first args))
                     (catch Exception _ default-num-events))
        client (create-client default-pulsar-url)
        producer (create-producer client default-topic)]

    (with-resources client producer
      #(do
         (log/log-flow :message {:message_type "Starting event publisher..."})
         (publish-events producer num-events)
         (log/log-flow :message {:message_type "Event publishing completed."})))))

;; Development/REPL helpers
(comment
  (def test-event
    {:entity-id 1
     :entity-type "Employee"
     :operation "Update"
     :timestamp (System/currentTimeMillis)
     :payload {:name "John Doe"}})

  (let [client (create-client default-pulsar-url)
        producer (create-producer client default-topic)]
    (publish-event producer test-event)
    (.close producer)
    (.close client)))