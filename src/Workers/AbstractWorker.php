<?php

declare(strict_types=1);

namespace Temporal\Samples\Workers;

use Temporal\WorkerFactory;
use Temporal\Worker\WorkerOptions;

abstract class AbstractWorker
{
    protected function getWorkerOptions(): WorkerOptions
    {
        return WorkerOptions::new()
            ->withMaxConcurrentActivityExecutionSize(50)
            ->withMaxConcurrentActivityTaskPollers(10);
    }

    abstract protected function getTaskQueueName(): string;
    
    abstract protected function getActivityImplementation(): object;

    public function run(): void
    {
        $factory = WorkerFactory::create();
        
        $worker = $factory->newWorker(
            $this->getTaskQueueName(),
            $this->getWorkerOptions()
        );

        $worker->registerActivityImplementations($this->getActivityImplementation());

        $factory->run();
    }
} 