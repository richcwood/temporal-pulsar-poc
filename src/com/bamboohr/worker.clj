(ns com.bamboohr.worker
  (:require [com.bamboohr.flow-logger :as log]
            [com.bamboohr.employee-workflow :as employee-workflow]
            [com.bamboohr.department-workflow :as department-workflow])
  (:import [io.temporal.client WorkflowClient WorkflowClientOptions]
           [io.temporal.serviceclient WorkflowServiceStubs]
           [io.temporal.worker WorkerFactory WorkerOptions]
           ;; Import the generated workflow classes
           [com.bamboohr.employee_workflow EmployeeWorkflowImpl]
           [com.bamboohr.department_workflow DepartmentWorkflowImpl])
  (:gen-class))

(defn -main [& args]
  (let [worker-id        (or (first args) "worker")
        task-queue       (or (second args) "employee-task-queue")
        run-duration-ms  (or (some-> (nth args 2) Long/parseLong) 60000)
        client-options   (.. (WorkflowClientOptions/newBuilder)
                             (setIdentity worker-id)
                             build)
        service          (WorkflowServiceStubs/newInstance)
        client           (WorkflowClient/newInstance service client-options)
        factory          (WorkerFactory/newInstance client)
        worker-options   (.. (WorkerOptions/newBuilder)
                             (setMaxConcurrentActivityExecutionSize 100)
                             build)
        worker           (.. factory
                             (newWorker task-queue worker-options))]

    ;; Set worker-id as environment variable
    (System/setProperty "WORKER_ID" worker-id)

    ;; Register workflows and activities
    (doto worker
      (.registerWorkflowImplementationTypes
       (into-array Class [EmployeeWorkflowImpl DepartmentWorkflowImpl]))
      (.registerActivitiesImplementations
       (into-array Object [employee-workflow/employee-activities-impl
                           department-workflow/department-activities-impl])))

    (log/log-flow :message {:message_type "Worker starting"
                            :worker_id    worker-id})

    (try
      (.start factory)
      (log/log-flow :message {:message_type "Worker started"
                              :worker_id    worker-id
                              :task_queue   task-queue})
      (Thread/sleep run-duration-ms)
      (finally
        (log/log-flow :message {:message_type "Worker shutting down"
                                :worker_id    worker-id})
        (.shutdown factory)
        (.awaitTermination factory 1 java.util.concurrent.TimeUnit/MINUTES)))))