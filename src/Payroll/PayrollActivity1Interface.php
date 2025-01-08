<?php

declare(strict_types=1);

namespace Temporal\Samples\Payroll;

use Temporal\Activity\ActivityInterface;
use Temporal\Activity\ActivityMethod;

#[ActivityInterface(prefix: "PayrollActivity1.")]
interface PayrollActivity1Interface
{
    #[ActivityMethod(name: "Activity1")]
    public function payrollActivity1(array $args): string;
}