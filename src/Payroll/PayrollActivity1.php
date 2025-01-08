<?php

declare(strict_types=1);

namespace Temporal\Samples\Payroll;

use Temporal\Activity;
use Temporal\Samples\FlowLogger;
use Temporal\Samples\Payroll\PayrollActivity1Interface;

#[ActivityInterface(prefix: "PayrollActivity1.")]
class PayrollActivity1 implements PayrollActivity1Interface
{
    public function payrollActivity1(array $args): string
    {
        $activityId = Activity::getInfo()->id;
        $flowLogger = new FlowLogger();
        $flowLogger->logFlow('receive', [
            'node_name' => 'Payroll Activity 1',
            'activity_id' => $activityId,
            'args' => $args
        ]);

        // Simulate processing
        usleep(random_int(50, 150) * 1000);

        // Random failure simulation
        if (random_int(1, 10) <= 2) {
            $flowLogger->logFlow('message', [
                'message_type' => 'PayrollActivity1 failed',
                'activity_id' => $activityId
            ]);
            throw new \Exception("Random failure in payroll-activity-1");
        }

        $flowLogger->logFlow('message', [
            'message_type' => 'PayrollActivity1 completed',
            'activity_id' => $activityId
        ]);

        return "Result from payroll-activity-1";
    }
}