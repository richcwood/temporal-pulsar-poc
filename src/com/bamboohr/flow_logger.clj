(ns com.bamboohr.flow-logger
  (:require
   [clojure.data.json :as json]
   [clojure.java.io :as io]))

(def default-logs-file-path "logs/flow.json")

(defn- date-format
  "Takes a date format string and returns date format object which can be used as an argument
   to the .format function to return a formatted string from a date"
  [date-format-string]
  ;; Note: java.text.SimpleDateFormat is not thread-safe.
  ;; A consequence of this is that you can't assign a SimpleDateFormat object to a variable
  ;; using `def` and then share it between multiple threads.
  ;; See: https://www.baeldung.com/java-simple-date-format#2-thread-safety
  (let [format (java.text.SimpleDateFormat. date-format-string)]
    (.setTimeZone format (java.util.TimeZone/getTimeZone "UTC"))
    format))

(defn-  format-current-time
  "Returns the current date as a string formatted with the default database date format"
  []
  (.format (date-format "yyyy-MM-dd'T'HH:mm:ss'Z'") (java.util.Date.)))

(defn- safe-str [obj]
  (try
    (str obj)
    (catch Exception _
      "<unserializable object>")))

(defn- log-to-json-file
  "Append a log entry to the JSON file specified by file-path"
  [log-entry file-path]
  (try
    (with-open [w (io/writer file-path :append true)]
      (.write w (str (json/write-str log-entry) "\n")))
    (catch Exception e
      (prn :error
           ::log-to-json-file
           "Failed to write to JSON log file"
           {:error (safe-str e)
            :file-path file-path}))))

(defn log-flow
  "Logs agent flow information to a JSON file at the path specified by FLOW_LOGS_PATH.
   If env is not set, logs will be written to the default file path.
   
   Parameters:
   - log-type: A keyword indicating the log message type [:publish|:subscribe|:receive|:acknowledge|:message]
   - data: A map containing variable data related to the log entry"
  [log-type data]
  (let [file-path (or (System/getenv "FLOW_LOGS_PATH") default-logs-file-path)
        timestamp (format-current-time)
        log-entry {:log_type (name log-type)
                   :timestamp timestamp
                   :data data}]
    (log-to-json-file log-entry file-path)))