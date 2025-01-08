<?php

namespace Temporal\Samples;

use Exception;

class FlowLogger
{
    private const DEFAULT_LOG_FILE = '/app/logs/php-flow.json';
    
    private string $logFile;
    
    public function __construct(
        private string $logFilePath = self::DEFAULT_LOG_FILE
    ) {
        $this->logFile = $logFilePath;
        $this->initializeLogFile();
    }

    private function initializeLogFile(): void
    {
        // Create logs directory if it doesn't exist
        $logDir = dirname($this->logFile);
        if (!is_dir($logDir)) {
            mkdir($logDir, 0777, true);
        }

        // Create log file if it doesn't exist
        if (!file_exists($this->logFile)) {
            file_put_contents($this->logFile, '');
        }
    }

    private function formatCurrentTime(): string
    {
        return gmdate('Y-m-d\TH:i:s\Z');
    }

    private function safeSerialize(mixed $obj): string
    {
        try {
            return (string) $obj;
        } catch (Exception $e) {
            return '<unserializable object>';
        }
    }

    /**
     * Logs agent flow information to a JSON file
     *
     * @param string $logType The type of log message (publish|subscribe|receive|acknowledge|message)
     * @param array $data Additional data related to the log entry
     * @return void
     */
    public function logFlow(string $logType, array $data): void
    {
        $logEntry = [
            'log_type' => $logType,
            'timestamp' => $this->formatCurrentTime(),
            'data' => $data
        ];

        try {
            $jsonEntry = json_encode($logEntry) . "\n";
            file_put_contents($this->logFile, $jsonEntry, FILE_APPEND);
        } catch (Exception $e) {
            // You might want to handle file writing errors according to your needs
            error_log("Failed to write log entry: " . $e->getMessage());
        }
    }
} 