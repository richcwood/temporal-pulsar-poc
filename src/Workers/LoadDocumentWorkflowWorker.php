<?php

declare(strict_types=1);

namespace Temporal\Samples\Workers;

require __DIR__ . '/../../vendor/autoload.php';

use Temporal\WorkerFactory;
use Temporal\Samples\LoadDocumentWorkflow\LoadDocumentWorkflow;

// Create a worker factory and worker
$factory = WorkerFactory::create();
$worker = $factory->newWorker('load-document-workflow-task-queue');

// Register the LoadDocumentWorkflow
$worker->registerWorkflowTypes(LoadDocumentWorkflow::class);

// Run the worker
$factory->run();