<?php

declare(strict_types=1);

namespace Temporal\Samples\LoadDocumentWorkflow;

use Temporal\Activity\ActivityInterface;
use Temporal\Activity\ActivityMethod;

#[ActivityInterface(prefix: "ReportResultActivity.")]
interface ReportResultActivityInterface
{
    #[ActivityMethod(name: "ReportResult")]
    public function reportResult(string $loadDocumentResult): string;
}
