<?php

declare(strict_types=1);

namespace Temporal\Samples\LoadDocumentWorkflow;

use Temporal\Activity\ActivityOptions;
use Temporal\DataConverter\BinaryConverter;
use Temporal\Samples\FlowLogger;
use Temporal\Workflow;
use Temporal\Workflow\WorkflowInterface;
use Temporal\Workflow\WorkflowMethod;
use Temporal\Common\RetryOptions;
use Carbon\CarbonInterval;

class LoadDocumentWorkflow implements LoadDocumentWorkflowInterface
{
    #[WorkflowMethod(name: "load-document-to-askbamboohr")]
    public function handler(array $args)
    {
        $flowLogger = new FlowLogger();
        $flowLogger->logFlow('receive', [
            'node_name' => 'Load Document Workflow',
            'args' => $args
        ]);

        // Define activity options
        $activityOptions = ActivityOptions::new()
            ->withStartToCloseTimeout(CarbonInterval::seconds(10))
            ->withTaskQueue('load-document-task-queue')
            ->withRetryOptions(
                RetryOptions::new()
                    ->withMaximumAttempts(3)
            );

        $reportResultActivityOptions = ActivityOptions::new()
            ->withStartToCloseTimeout(CarbonInterval::seconds(10))
            ->withTaskQueue('report-results-task-queue')
            ->withRetryOptions(
                RetryOptions::new()
                    ->withMaximumAttempts(3)
            );

        // Create activity stubs
        $loadDocumentActivity = Workflow::newUntypedActivityStub($activityOptions);
        $reportResultActivity = Workflow::newActivityStub(
            ReportResultActivityInterface::class,
            $reportResultActivityOptions
        );

        $customDataConverter = new BinaryConverter();

        // Execute activities
        try {
            // Call the load-document activity implemented in Clojure
            $activityParameters = ["document_id" => "123456"];
            $serializedArgs = $customDataConverter->toPayload($activityParameters);
            $flowLogger->logFlow('publish', [
                'node' => 'Load Document Workflow',
                'target_node' => 'Load Document Activity'
            ]);
            $loadDocumentResult = yield $loadDocumentActivity->execute('load-document', [$serializedArgs]);

            // Call the report-result activity implemented in PHP
            $flowLogger->logFlow('publish', [
                'node' => 'Load Document Activity',
                'target_node' => 'Report Result Activity'
            ]);
            $reportResult = yield $reportResultActivity->reportResult($loadDocumentResult);
        } catch (\Throwable $e) {
            $flowLogger->logFlow('message', [
                'message_type' => 'LoadDocumentToAskBambooHRWorkflow failed',
                'error_message' => $e->getMessage()
            ]);
            throw $e;
        }

        $flowLogger->logFlow('message', [
            'message_type' => 'LoadDocumentToAskBambooHRWorkflow completed',
            'report_result' => $reportResult
        ]);

        return $reportResult;
    }
}