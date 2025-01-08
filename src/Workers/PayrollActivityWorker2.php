<?php

declare(strict_types=1);

namespace Temporal\Samples\Workers;

require __DIR__ . '/../../vendor/autoload.php';

use Temporal\WorkerFactory;
use Temporal\Samples\Payroll\PayrollActivity2;
use Temporal\Worker\WorkerOptions;

class PayrollActivityWorker2 extends AbstractWorker
{
    protected function getTaskQueueName(): string
    {
        return 'payroll-task-queue-2';
    }

    protected function getActivityImplementation(): object
    {
        return new PayrollActivity2();
    }
}

// Instantiate and run the worker
(new PayrollActivityWorker2())->run();
