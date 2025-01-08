<?php

declare(strict_types=1);

namespace Temporal\Samples\Payroll;

use Temporal\Workflow\WorkflowInterface;
use Temporal\Workflow\WorkflowMethod;

#[WorkflowInterface]
interface PayrollWorkflowInterface
{
    #[WorkflowMethod(name: "payroll-workflow")]
    public function handler(array $args);
}