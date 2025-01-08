#!/bin/bash

# Define run time in milliseconds
RUN_TIME_MS=70000
NUM_EVENT_MESSAGES_TO_PUBLISH=500
NUM_PAYROLL_WORKFLOWS=100
NUM_LOAD_DOCUMENT_WORKFLOWS=100

# Function to convert milliseconds to seconds with fractional part
function ms_to_s() {
  echo "scale=3; $1/1000" | bc
}
RUN_TIME_S=$(ms_to_s $RUN_TIME_MS)

# Start Event Handlers
echo "Starting Event Handler 1..."
clj -m com.bamboohr.event-handler $RUN_TIME_MS "event-handler-1" &
EVENT_HANDLER_1_PID=$!

echo "Starting Event Handler 2..."
clj -m com.bamboohr.event-handler $RUN_TIME_MS "event-handler-2" &
EVENT_HANDLER_2_PID=$!

# Start Clojure Workers
echo "Starting Employee Worker 1..."
clj -m com.bamboohr.worker "employee-worker-1" "employee-task-queue" $RUN_TIME_MS &
EMPLOYEE_WORKER_1_PID=$!

echo "Starting Department Worker 1..."
clj -m com.bamboohr.worker "department-worker-1" "department-task-queue" $RUN_TIME_MS &
DEPARTMENT_WORKER_1_PID=$!

# Start PHP Workers
echo "Starting Payroll Activity Worker 1..."
gtimeout $RUN_TIME_S docker compose -f deploy/temporal/docker-compose/docker-compose.yml exec -T php-worker ./rr serve -c deploy/php/PayrollActivityWorker1.yaml &
PAYROLL_WORKER_1_PID=$!

echo "Starting Payroll Activity Worker 2..."
gtimeout $RUN_TIME_S docker compose -f deploy/temporal/docker-compose/docker-compose.yml exec -T php-worker ./rr serve -c deploy/php/PayrollActivityWorker2.yaml &
PAYROLL_WORKER_2_PID=$!

echo "Starting Payroll Workflow Worker..."
gtimeout $RUN_TIME_S docker compose -f deploy/temporal/docker-compose/docker-compose.yml exec -T php-worker ./rr serve -c deploy/php/PayrollWorkflowWorker.yaml &
PAYROLL_WORKFLOW_WORKER_PID=$!

echo "Starting Report Result Worker..."
gtimeout $RUN_TIME_S docker compose -f deploy/temporal/docker-compose/docker-compose.yml exec -T php-worker ./rr serve -c deploy/php/ReportResultsWorker.yaml &
REPORT_RESULT_WORKER_PID=$!

echo "Starting Load Document Worker..."
clj -m com.bamboohr.load-document-worker "load-document-worker" "load-document-task-queue" $RUN_TIME_MS &
LOAD_DOCUMENT_WORKER_PID=$!

echo "Starting Load Document Workflow Worker..."
gtimeout $RUN_TIME_S docker compose -f deploy/temporal/docker-compose/docker-compose.yml exec -T php-worker ./rr serve -c deploy/php/LoadDocumentWorkflowWorker.yaml &
LOAD_DOCUMENT_WORKFLOW_WORKER_PID=$!

echo "Starting Event Publisher..."
clj -m com.bamboohr.event-publisher $NUM_EVENT_MESSAGES_TO_PUBLISH &

echo "Starting Load Document Workflow..."
gtimeout $RUN_TIME_S docker compose -f deploy/temporal/docker-compose/docker-compose.yml exec -T php-worker php start_workflows.php document $NUM_LOAD_DOCUMENT_WORKFLOWS &

echo "Starting Payroll Workflow..."
gtimeout $RUN_TIME_S docker compose -f deploy/temporal/docker-compose/docker-compose.yml exec -T php-worker php start_workflows.php payroll $NUM_PAYROLL_WORKFLOWS &

# Simulate scaling up of workers
echo "Scaling up workers on time delay..."
sleep 10
RUN_TIME_MS=$(( RUN_TIME_MS > 15000 ? RUN_TIME_MS - 10000 : 5000 ))
echo "Starting Employee Worker 2..."
clj -m com.bamboohr.worker "employee-worker-2" "employee-task-queue" $RUN_TIME_MS &
EMPLOYEE_WORKER_2_PID=$!

sleep 10
RUN_TIME_MS=$(( RUN_TIME_MS > 15000 ? RUN_TIME_MS - 10000 : 5000 ))
echo "Starting Employee Worker 3..."
clj -m com.bamboohr.worker "employee-worker-3" "employee-task-queue" $RUN_TIME_MS &
EMPLOYEE_WORKER_3_PID=$!

sleep 10
RUN_TIME_MS=$(( RUN_TIME_MS > 15000 ? RUN_TIME_MS - 10000 : 5000 ))
echo "Starting Employee Worker 4..."
clj -m com.bamboohr.worker "employee-worker-4" "employee-task-queue" $RUN_TIME_MS &
EMPLOYEE_WORKER_4_PID=$!

# Wait for all background processes to finish
wait $EVENT_HANDLER_1_PID
wait $EVENT_HANDLER_2_PID
wait $EMPLOYEE_WORKER_1_PID
wait $EMPLOYEE_WORKER_2_PID
wait $EMPLOYEE_WORKER_3_PID
wait $EMPLOYEE_WORKER_4_PID
wait $DEPARTMENT_WORKER_1_PID
wait $PAYROLL_WORKER_1_PID
wait $PAYROLL_WORKER_2_PID
wait $PAYROLL_WORKFLOW_WORKER_PID
wait $REPORT_RESULT_WORKER_PID
wait $LOAD_DOCUMENT_WORKER_PID
