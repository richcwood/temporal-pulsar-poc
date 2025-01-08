<?php

declare(strict_types=1);

namespace Temporal\Samples\Payroll;

use Temporal\Activity\ActivityInterface;
use Temporal\Activity\ActivityMethod;

#[ActivityInterface(prefix: "PayrollActivity2.")]
interface PayrollActivity2Interface
{
    #[ActivityMethod(name: "Activity2")]
    public function payrollActivity2(array $args): string;
}