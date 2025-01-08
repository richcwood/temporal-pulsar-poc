<?php

declare(strict_types=1);

namespace Temporal\Samples\Payroll;

use Temporal\Activity\ActivityOptions; 
use Temporal\Samples\FlowLogger;
use Temporal\Workflow;
use Temporal\Workflow\WorkflowInterface;
use Temporal\Workflow\WorkflowMethod;
use Temporal\Common\RetryOptions;
use Carbon\CarbonInterval;

/**
 * @Workflow\WorkflowInterface
 */
class PayrollWorkflow implements PayrollWorkflowInterface
{
    #[WorkflowMethod(name: "payroll-workflow")]
    public function handler(array $args)
    {
        $flowLogger = new FlowLogger();
        $flowLogger->logFlow('receive', [
            'node_name' => 'Payroll Workflow',
            'args' => $args
        ]);

        // Define activity options
        $activityOptions1 = ActivityOptions::new()
        ->withStartToCloseTimeout(CarbonInterval::seconds(5))
        ->withTaskQueue('payroll-task-queue-1')
        ->withRetryOptions(
            RetryOptions::new()
                ->withMaximumAttempts(3)
        );
 
        $activityOptions2 = ActivityOptions::new()
        ->withStartToCloseTimeout(CarbonInterval::seconds(5))
        ->withTaskQueue('payroll-task-queue-2')
        ->withRetryOptions(
            RetryOptions::new()
                ->withMaximumAttempts(3)
        );

        // Create activity stubs
        $activities1 = Workflow::newActivityStub(
            PayrollActivity1Interface::class,
            $activityOptions1
        );

        $activities2 = Workflow::newActivityStub(
            PayrollActivity2Interface::class,
            $activityOptions2
        );

        // Execute activities
        try {
            $flowLogger->logFlow('publish', [
                'node' => 'Payroll Workflow',
                'target_node' => 'Payroll Activity 1'
            ]);
            $result1 = yield $activities1->payrollActivity1($args);
            $flowLogger->logFlow('publish', [
                'node' => 'Payroll Activity 1',
                'target_node' => 'Payroll Activity 2'
            ]);
            $result2 = yield $activities2->payrollActivity2($args);
        } catch (\Throwable $e) {
            $flowLogger->logFlow('message', [
                'message_type' => 'PayrollWorkflow failed',
                'error_message' => $e->getMessage()
            ]);
            throw $e;
        }

        $flowLogger->logFlow('message', [
            'message_type' => 'PayrollWorkflow completed',
            'result1' => $result1,
            'result2' => $result2,
        ]);

        return [
            'result1' => $result1,
            'result2' => $result2,
        ];
    }
}