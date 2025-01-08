(ns com.bamboohr.employee-workflow
  (:require [com.bamboohr.flow-logger :as log])
  (:import [io.temporal.activity ActivityOptions Activity]
           [io.temporal.common RetryOptions]
           [io.temporal.workflow Workflow]
           [java.time Duration]
           [com.bamboohr.interfaces.employee_activities EmployeeActivities])
  (:gen-class
   :name com.bamboohr.employee_workflow.EmployeeWorkflowImpl
   :implements [com.bamboohr.interfaces.employee_workflow.EmployeeWorkflow]))


;; Employee Activities Implementation
(def employee-activities-impl
  (reify EmployeeActivities
    (employeeActivity1 [_ args]
      (let [worker-id (System/getProperty "WORKER_ID")
            _ (log/log-flow :receive {:node_name "Employee Activity 1" :worker_id worker-id})
            _ (log/log-flow :publish {:node "Employee Activity 1" :target_node worker-id})
            _ (log/log-flow :receive {:node_name worker-id})
            failure?  (< (rand) 0.2)]
        (Thread/sleep (+ 50 (rand-int 100)))
        (if failure?
          (throw (ex-info "Random failure in employeeActivity1" {}))
          (str "employeeActivity1 completed with args: " args))))
    (employeeActivity2 [_ args]
      (let [worker-id (System/getProperty "WORKER_ID")
            _ (log/log-flow :receive {:node_name "Employee Activity 2" :worker_id worker-id})
            _ (log/log-flow :publish {:node "Employee Activity 2" :target_node worker-id})
            _ (log/log-flow :receive {:node_name worker-id})
            failure?  (< (rand) 0.2)]
        (Thread/sleep (+ 50 (rand-int 100)))
        (if failure?
          (throw (ex-info "Random failure in employeeActivity2" {}))
          (str "employeeActivity2 completed with args: " args))))
    (employeeActivity3 [_ args]
      (let [worker-id (System/getProperty "WORKER_ID")
            _ (log/log-flow :receive {:node_name "Employee Activity 3" :worker_id worker-id})
            _ (log/log-flow :publish {:node "Employee Activity 3" :target_node worker-id})
            _ (log/log-flow :receive {:node_name worker-id})
            failure?  (< (rand) 0.2)]
        (Thread/sleep (+ 50 (rand-int 100)))
        (if failure?
          (throw (ex-info "Random failure in employeeActivity3" {}))
          (str "employeeActivity3 completed with args: " args))))))

;; Employee Workflow Implementation
(defn -execute
  [_ args]
  (let [retry-opts    (-> (RetryOptions/newBuilder)
                          (.setInitialInterval (Duration/ofSeconds 1))
                          (.setMaximumInterval (Duration/ofSeconds 10))
                          (.setBackoffCoefficient 2.0)
                          (.setMaximumAttempts 3)
                          .build)
        activity-opts (-> (ActivityOptions/newBuilder)
                          (.setStartToCloseTimeout (Duration/ofSeconds 30))
                          (.setRetryOptions retry-opts)
                          .build)
        activities    (Workflow/newActivityStub
                       com.bamboohr.interfaces.employee_activities.EmployeeActivities
                       activity-opts)]
    (try
      (let [_ (log/log-flow :receive {:node_name "Employee Workflow"})
            _ (log/log-flow :publish {:node "Employee Workflow" :target_node "Employee Activity 1"})
            result1 (.employeeActivity1 activities args)
            _ (log/log-flow :publish {:node "Employee Activity 1" :target_node "Employee Activity 2"})
            result2 (.employeeActivity2 activities args)
            _ (log/log-flow :publish {:node "Employee Activity 2" :target_node "Employee Activity 3"})
            result3 (.employeeActivity3 activities args)]
        {"result1" result1
         "result2" result2
         "result3" result3})
      (catch Exception e
        (log/log-flow :message {:message_type "employeeWorkflow failed"
                                :error        (.getMessage e)})
        (throw e)))))