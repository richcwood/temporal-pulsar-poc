<?php

declare(strict_types=1);

namespace Temporal\Samples\Workers;

require __DIR__ . '/../../vendor/autoload.php';

use Temporal\WorkerFactory;
use Temporal\Samples\LoadDocumentWorkflow\ReportResultActivity;
use Temporal\Worker\WorkerOptions;

class ReportResultsWorker extends AbstractWorker
{
    protected function getTaskQueueName(): string
    {
        return 'report-results-task-queue';
    }

    protected function getActivityImplementation(): object
    {
        return new ReportResultActivity();
    }
}

// Instantiate and run the worker
(new ReportResultsWorker())->run();