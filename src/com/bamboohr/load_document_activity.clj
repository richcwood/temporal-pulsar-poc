(ns com.bamboohr.load-document-activity
  (:require [com.bamboohr.flow-logger :as log])
  (:import [io.temporal.activity Activity]
           [com.bamboohr.interfaces.load_document_activity LoadDocumentActivity]))

(defn load-document-activity []
  (reify LoadDocumentActivity
    (loadDocument [_ args]
      (let [activity-info (-> (Activity/getExecutionContext) .getInfo)
            activity-id (.getActivityId activity-info)
            _ (log/log-flow :receive {:node_name "Load Document Activity"
                                      :worker_id activity-id
                                      :args args})]
        ;; Simulate processing
        (Thread/sleep (+ 50 (rand-int 100)))
        ;; Simulate success
        (log/log-flow :message {:message_type "LoadDocumentActivity completed"
                                :worker_id activity-id})
        "Load Document Result")))) 