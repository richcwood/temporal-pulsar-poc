<?php

declare(strict_types=1);

namespace Temporal\Samples\Workers;

require __DIR__ . '/../../vendor/autoload.php';

use Temporal\WorkerFactory;
use Temporal\Samples\Payroll\PayrollActivity1;
use Temporal\Worker\WorkerOptions;

class PayrollActivityWorker1 extends AbstractWorker
{
    protected function getTaskQueueName(): string
    {
        return 'payroll-task-queue-1';
    }

    protected function getActivityImplementation(): object
    {
        return new PayrollActivity1();
    }
}

// Instantiate and run the worker
(new PayrollActivityWorker1())->run();
