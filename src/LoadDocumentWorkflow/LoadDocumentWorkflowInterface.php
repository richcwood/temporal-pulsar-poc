<?php

declare(strict_types=1);

namespace Temporal\Samples\LoadDocumentWorkflow;

use Temporal\Workflow\WorkflowInterface;
use Temporal\Workflow\WorkflowMethod;

#[WorkflowInterface]
interface LoadDocumentWorkflowInterface
{
    #[WorkflowMethod(name: "load-document-to-askbamboohr")]
    public function handler(array $args);
}