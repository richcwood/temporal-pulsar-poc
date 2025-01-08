(ns com.bamboohr.event-handler
  (:require [com.bamboohr.flow-logger :as log])
  (:import [io.temporal.client WorkflowClient WorkflowOptions WorkflowStub]
           [io.temporal.serviceclient WorkflowServiceStubs WorkflowServiceStubsOptions]
           [org.apache.pulsar.client.api PulsarClient Schema SubscriptionType
            SubscriptionInitialPosition PulsarClientException$TimeoutException]
           [java.util Map]
           [java.util.concurrent TimeUnit]
           [com.bamboohr.interfaces.employee_workflow EmployeeWorkflow]
           [com.bamboohr.interfaces.department_workflow DepartmentWorkflow]))

(defn- get-config []
  {:pulsar-url "pulsar://localhost:6650"
   :topic {:event-handler "persistent://public/default/event-handler"}
   :subscription {:name "event-handler-sub"
                  :type SubscriptionType/Shared
                  :position SubscriptionInitialPosition/Earliest}})

(defn create-client [url]
  (-> (PulsarClient/builder)
      (.serviceUrl url)
      .build))

(defn create-consumer [client topic subscription-config]
  (-> client
      (.newConsumer (Schema/JSON Map))
      (.topic (into-array String [topic]))
      (.subscriptionName (:name subscription-config))
      (.subscriptionType (:type subscription-config))
      .subscribe))

(defn create-temporal-client []
  (let [service-options (-> (WorkflowServiceStubsOptions/newBuilder)
                            (.setTarget "127.0.0.1:7233")
                            (.setRpcTimeout (java.time.Duration/ofSeconds 20))
                            .build)
        service (WorkflowServiceStubs/newInstance service-options)
        client (WorkflowClient/newInstance service)]
    {:service service
     :temporal-client client}))

(defn handle-message [consumer client event-handler-id msg]
  (when msg
    (let [event (.getValue msg)
          entity-type (get event "entity-type")]
      (log/log-flow :message (merge {:message_type "Received event"} event))
      (log/log-flow :receive {:node_name event-handler-id})
      (log/log-flow :publish {:node "Event Publisher" :target_node event-handler-id})
      (try
        (cond
          (= entity-type "Employee")
          (do
            (log/log-flow :message (merge {:message_type "Starting Employee Workflow"} event))
            ;; Create an untyped workflow stub and start the workflow asynchronously
            (let [options (-> (WorkflowOptions/newBuilder)
                              (.setTaskQueue "employee-task-queue")
                              .build)
                  workflow-stub (.newUntypedWorkflowStub client "EmployeeWorkflow" options)
                  args (object-array [(str event)])
                  _ (log/log-flow :publish {:node event-handler-id :target_node "Employee Workflow"})]
              (.start workflow-stub args)
              (log/log-flow :message {:message_type "Employee Workflow started."})))

          (= entity-type "Department")
          (do
            (log/log-flow :message (merge {:message_type "Starting Department Workflow"} event))
            ;; Create an untyped workflow stub and start the workflow asynchronously
            (let [options (-> (WorkflowOptions/newBuilder)
                              (.setTaskQueue "department-task-queue")
                              .build)
                  workflow-stub (.newUntypedWorkflowStub client "DepartmentWorkflow" options)
                  args (object-array [(str event)])
                  _ (log/log-flow :publish {:node event-handler-id :target_node "Department Workflow"})]
              (.start workflow-stub args)
              (log/log-flow :message {:message_type "Department Workflow started."})))

          :else
          (log/log-flow :message {:message_type "No matching workflow for entity type"
                                  :entity_type entity-type}))

        ;; Acknowledge the message after starting the workflow
        (.acknowledge consumer msg)

        (catch Exception e
          (log/log-flow :message {:message_type "Error starting workflow"
                                  :error (.getMessage e)}))))))

(defn process-events [config run-duration-ms]
  (let [event-handler-id (get-in config [:event-handler-id])
        client (create-client (:pulsar-url config))
        consumer (create-consumer client
                                  (get-in config [:topic :event-handler])
                                  (:subscription config))
        {:keys [service temporal-client]} (create-temporal-client)
        end-time (+ (System/currentTimeMillis) run-duration-ms)
        max-timeout 1000]  ;; Maximum timeout for receive in milliseconds
    (try
      ;; Loop to process messages from Pulsar
      (loop []
        (let [remaining-time (- end-time (System/currentTimeMillis))
              timeout (long (min remaining-time max-timeout))]
          (when (pos? timeout)
            (try
              (handle-message consumer temporal-client event-handler-id
                              (.receive consumer timeout TimeUnit/MILLISECONDS))
              (catch PulsarClientException$TimeoutException _
                ;; Timeout occurred, continue the loop
                nil))
            (recur))))
      (finally
        ;; Properly close Pulsar resources
        (doseq [closeable [consumer client]]
          (.close closeable))
        ;; Properly shut down Temporal client and service
        (.shutdown service)
        (log/log-flow :message {:message_type "Event handler completed"})))))

(defn -main [& args]
  (let [run-duration-ms (Integer/parseInt (or (first args) "60000"))
        event-handler-id (or (second args) "event-handler")]
    (log/log-flow :message {:message_type "Event handler started"
                            :event-handler-id event-handler-id})
    (-> (get-config)
        (assoc-in [:event-handler-id] event-handler-id)
        (process-events run-duration-ms))))