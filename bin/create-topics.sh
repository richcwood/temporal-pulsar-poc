#!/bin/bash

BROKER="broker"
PULSAR_ADMIN="bin/pulsar-admin"
TOPICS=(
  "event-handler"
  "flow-logs"
)

# Delete existing topics
for topic in "${TOPICS[@]}"; do
  if [ "$topic" == "worker-queue" ] || [ "$topic" == "event-handler" ]; then
    docker exec $BROKER $PULSAR_ADMIN topics delete-partitioned-topic -f "persistent://public/default/$topic"
  else
    docker exec $BROKER $PULSAR_ADMIN topics delete -f "persistent://public/default/$topic"
  fi
done

# Create topics
for topic in "${TOPICS[@]}"; do
  if [ "$topic" == "worker-queue" ] || [ "$topic" == "event-handler" ]; then
    docker exec $BROKER $PULSAR_ADMIN topics create-partitioned-topic "persistent://public/default/$topic" -p 3
  else
    docker exec $BROKER $PULSAR_ADMIN topics create "persistent://public/default/$topic"
  fi
done


# docker exec broker bin/pulsar-admin topics delete -f persistent://public/default/event-bus
# docker exec broker bin/pulsar-admin topics delete -f persistent://public/default/work-queue
# docker exec broker bin/pulsar-admin topics delete -f persistent://public/default/task-queue-1
# docker exec broker bin/pulsar-admin topics delete -f persistent://public/default/task-queue-2
# docker exec broker bin/pulsar-admin topics delete -f persistent://public/default/worker-queue
# docker exec broker bin/pulsar-admin topics create persistent://public/default/event-bus
# docker exec broker bin/pulsar-admin topics create persistent://public/default/work-queue
# docker exec broker bin/pulsar-admin topics create persistent://public/default/task-queue-1
# docker exec broker bin/pulsar-admin topics create persistent://public/default/task-queue-2
# docker exec broker bin/pulsar-admin topics create-partitioned-topic persistent://public/default/worker-queue -p 3
