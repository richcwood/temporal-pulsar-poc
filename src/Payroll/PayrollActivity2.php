<?php

declare(strict_types=1);

namespace Temporal\Samples\Payroll;

use Temporal\Activity;
use Temporal\Samples\FlowLogger;
use Temporal\Samples\Payroll\PayrollActivity2Interface;

#[ActivityInterface(prefix: "PayrollActivity2.")]
class PayrollActivity2 implements PayrollActivity2Interface
{
    public function payrollActivity2(array $args): string
    {
        $activityId = Activity::getInfo()->id;
        $flowLogger = new FlowLogger();
        $flowLogger->logFlow('receive', [
            'node_name' => 'Payroll Activity 2',
            'activity_id' => $activityId,
            'args' => $args
        ]);

        // Simulate processing
        usleep(random_int(50, 150) * 1000);

        // Random failure simulation
        if (random_int(1, 10) <= 2) {
            $flowLogger->logFlow('message', [
                'message_type' => 'PayrollActivity2 failed',
                'activity_id' => $activityId
            ]);
            throw new \Exception("Random failure in payroll-activity-2");
        }

        $flowLogger->logFlow('message', [
            'message_type' => 'PayrollActivity2 completed',
            'activity_id' => $activityId
        ]);

        return "Result from payroll-activity-2";
    }
}