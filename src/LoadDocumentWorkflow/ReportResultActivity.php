<?php

declare(strict_types=1);

namespace Temporal\Samples\LoadDocumentWorkflow;

use Temporal\Activity;
use Temporal\Samples\FlowLogger;
use Temporal\Samples\LoadDocumentWorkflow\ReportResultActivityInterface;

#[ActivityInterface(prefix: "ReportResultActivity.")]
class ReportResultActivity implements ReportResultActivityInterface
{
    public function reportResult(string $loadDocumentResult): string
    {
        $activityId = Activity::getInfo()->id;
        $flowLogger = new FlowLogger();
        $flowLogger->logFlow('receive', [
            'node_name' => 'Report Result Activity',
            'activity_id' => $activityId,
            'args' => $loadDocumentResult
        ]);

        // Simulate processing
        usleep(random_int(50, 150) * 1000);

        $flowLogger->logFlow('message', [
            'message_type' => 'ReportResultActivity completed',
            'activity_id' => $activityId
        ]);

        return "Report Result: Success";
    }
}