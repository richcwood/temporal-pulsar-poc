(ns com.bamboohr.department-workflow
  (:require [com.bamboohr.flow-logger :as log])
  (:import [io.temporal.activity ActivityOptions Activity]
           [io.temporal.common RetryOptions]
           [io.temporal.workflow Workflow]
           [java.time Duration]
           [com.bamboohr.interfaces.department_activities DepartmentActivities])
  (:gen-class
   :name com.bamboohr.department_workflow.DepartmentWorkflowImpl
   :implements [com.bamboohr.interfaces.department_workflow.DepartmentWorkflow]))

;; Department Activities Implementation
(def department-activities-impl
  (reify DepartmentActivities
    (departmentActivity1 [_ args]
      (let [worker-id (System/getProperty "WORKER_ID")
            _ (log/log-flow :receive {:node_name "Department Activity 1" :worker_id worker-id})
            _ (log/log-flow :publish {:node "Department Activity 1" :target_node worker-id})
            _ (log/log-flow :receive {:node_name worker-id})
            failure?  (< (rand) 0.2)]
        (Thread/sleep (+ 50 (rand-int 100)))
        (if failure?
          (throw (ex-info "Random failure in departmentActivity1" {}))
          (str "departmentActivity1 completed with args: " args))))))

;; Department Workflow Implementation
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
                       com.bamboohr.interfaces.department_activities.DepartmentActivities
                       activity-opts)]
    (try
      (let [_ (log/log-flow :receive {:node_name "Department Workflow"})
            _ (log/log-flow :publish {:node "Department Workflow" :target_node "Department Activity 1"})
            result1 (.departmentActivity1 activities args)]
        {"result1" result1})
      (catch Exception e
        (log/log-flow :message {:message_type "departmentWorkflow failed"
                                :error        (.getMessage e)})
        (throw e)))))