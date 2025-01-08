<?php

declare(strict_types=1);

namespace Temporal\Samples\Workers;

require __DIR__ . '/../../vendor/autoload.php';

use Temporal\WorkerFactory;
use Temporal\Samples\Payroll\PayrollWorkflow;
use Temporal\Samples\Payroll\PayrollWorkflowInterface;

// Create a worker factory and worker
$factory = WorkerFactory::create();
$worker = $factory->newWorker('payroll-task-queue');

// Register the PayrollWorkflow
$worker->registerWorkflowTypes(PayrollWorkflow::class);

// Run the worker
$factory->run();