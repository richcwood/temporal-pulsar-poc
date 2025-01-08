<?php

declare(strict_types=1);

require 'vendor/autoload.php';

use Temporal\Client\WorkflowClient;
use Temporal\Client\GRPC\ServiceClient;
use Temporal\SampleUtils\WorkflowStarter;
use Temporal\Samples\FlowLogger;
use Temporal\Samples\LoadDocumentWorkflow\LoadDocumentWorkflowInterface;
use Temporal\Samples\Payroll\PayrollWorkflowInterface;
use Temporal\Client\WorkflowOptions;
use Carbon\CarbonInterval;
use DateTime;

// Get workflow type and number from command line arguments
if ($argc < 3) {
    die("Usage: php start_workflows.php <workflow_type> <num_workflows>\n" .
        "workflow_type: 'document' or 'payroll'\n");
}

$workflowType = strtolower($argv[1]);
$numWorkflows = (int)$argv[2];

if (!in_array($workflowType, ['document', 'payroll'])) {
    die("Invalid workflow type. Use 'document' or 'payroll'\n");
}

$client = WorkflowClient::create(ServiceClient::create('temporal:7233'));
$flowLogger = new FlowLogger();

$workflowConfig = [
    'document' => [
        'interface' => LoadDocumentWorkflowInterface::class,
        'task_queue' => 'load-document-workflow-task-queue',
        'prefix' => 'load-document-workflow',
        'message' => 'LoadDocumentWorkflow'
    ],
    'payroll' => [
        'interface' => PayrollWorkflowInterface::class,
        'task_queue' => 'payroll-task-queue',
        'prefix' => 'payroll-workflow',
        'message' => 'PayrollWorkflow'
    ]
];

$config = $workflowConfig[$workflowType];

$flowLogger->logFlow('message', [
    'message_type' => "Starting {$config['message']}s",
    'num_workflows' => $numWorkflows
]);

for ($i = 0; $i < $numWorkflows; $i++) {
    $workflowId = sprintf(
        '%s-%s-%d',
        $config['prefix'],
        (new DateTime())->format('Y-m-d-H-i-s-u'),
        $i
    );
    
    $workflow = $client->newWorkflowStub(
        $config['interface'],
        WorkflowOptions::new()
            ->withWorkflowId($workflowId)
            ->withTaskQueue($config['task_queue'])
            ->withWorkflowExecutionTimeout(CarbonInterval::minutes(5))
    );

    $workflowArgs = $workflowType === 'payroll' ? ['workflowNumber' => $i] : [];
    $client->start($workflow, $workflowArgs);

    $flowLogger->logFlow('message', [
        'message_type' => "{$config['message']} started",
        'workflow_id' => $workflowId
    ]);
}

$flowLogger->logFlow('message', [
    'message_type' => "All {$config['message']}s started"
]); 