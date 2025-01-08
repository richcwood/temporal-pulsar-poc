(ns com.bamboohr.load-document-worker
  (:require [com.bamboohr.flow-logger :as log]
            [com.bamboohr.load-document-activity :refer [load-document-activity]])
  (:import [io.temporal.worker WorkerFactory]
           [io.temporal.client WorkflowClient]
           [io.temporal.serviceclient WorkflowServiceStubs WorkflowServiceStubsOptions]))

(defn -main [& args]
  (let [worker-id        (or (first args) "worker")
        task-queue       (or (second args) "load-document-task-queue")
        run-duration-ms  (or (some-> (nth args 2) Long/parseLong) 60000)
        ;; Configure service stubs with longer timeout and specific target
        service-options  (-> (WorkflowServiceStubsOptions/newBuilder)
                             (.setTarget "127.0.0.1:7233")  ; default Temporal server address
                             (.setRpcTimeout (java.time.Duration/ofSeconds 20))
                             (.build))
        service          (WorkflowServiceStubs/newInstance service-options)
        client          (WorkflowClient/newInstance service)
        factory         (WorkerFactory/newInstance client)
        worker          (.newWorker factory task-queue)]

    (log/log-flow :message {:message_type "Worker starting"
                            :worker_id    worker-id})

    ;; Register the activity implementation as an array
    (.registerActivitiesImplementations worker (into-array Object [(load-document-activity)]))

    ;; Start the worker
    (.start factory)

    (log/log-flow :message {:message_type "Worker started"
                            :worker_id    worker-id
                            :task_queue   task-queue})

    ;; Keep the worker running for the specified duration
    (Thread/sleep run-duration-ms)

    (log/log-flow :message {:message_type "Worker shutting down"
                            :worker_id    worker-id})

    ;; Shutdown the worker
    (.shutdown factory)
    (.awaitTermination factory 10 java.util.concurrent.TimeUnit/SECONDS)))