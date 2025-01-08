# Temporal.io Simulation

This is a POC for a Temporal.io simulation that uses Apache Pulsar as an event bus. It is not intended to be used in production.

## Install prerequisites

```
$ brew install coreutils
```

## Pulsar Manager setup

1. Start pulsar manager in docker container

```
$ cd deploy/pulsar
$ docker compose up -d
```

2. Create a super user

```
$ cd deploy/pulsar/docker
$ ./bin/setup-super-user.sh
```

3. Log in to pulsar manager web UI

```
http://localhost:9527
username: admin
password: apachepulsar
```

4. Create a new Pulsar environment
    - Shell into the pulsar broker
    ```
    $ docker exec -it broker /bin/bash
    ``` 
    - Run `hostname -i` and copy the host ip address
    - Click `+ New Environment`
    - Fill in the following values:
        - Environment Name: `Local dev`
        - Service URL: `http://[broker host ip address]:8080`
        - Bookie URL: `http://[broker host ip address]:6650`

5. Start pulsar manager in docker container

  - Start docker container
    ```
    $ docker run -it \
      -p 9527:9527 -p 7750:7750 \
      -e SPRING_CONFIGURATION_FILE=/pulsar-manager/pulsar-manager/application.properties \
      --network pulsar_pulsar \
      apachepulsar/pulsar-manager:latest
    ```
  - Create the login user
    ```
    $ ./bin/setup-super-user.sh
    ```

6. Useful pulsar admin commands

  - First log in to pulsar broker
    - `docker exec -it broker /bin/bash`
  - Run `bin/pulsar-admin` to see all commands
  - Run `bin/pulsar-admin topics list public/default` to list all topics
  - Run `bin/pulsar-admin topics delete-partitioned-topic -f "persistent://public/default/event-handler"` to delete a partitioned topic
  - Run `bin/pulsar-admin topics create-partitioned-topic "persistent://public/default/event-handler" -p 3` to create a partitioned topic
  - Run `bin/pulsar-admin topics create "persistent://public/default/event-handler"` to create a non-partitioned topic
  - Reset consumer subscription cursor position to the earliest message
    - `bin/pulsar-admin topics reset-cursor persistent://public/default/flow-logs -s log-writer-subscription -m 'earliest'`
  
## Temporal setup

1. Clone the temporal docker repo

```
$ cd deploy/temporal
$ git clone https://github.com/temporalio/docker-compose.git
```

2. Replace the `docker-compose.yml` file with the one defined in this README (below)

3. Build the PHP worker base image

```
$ ./bin/build-php-worker.sh
```

4. Start the temporal services in docker

```
$ ./bin/start-temporal-services.sh
```

5. Log in to temporal web UI

```
http://localhost:8081
```

## Run the simulation

```
$ ./bin/run-simulation.sh
```

## Stop the temporal services

```
$ ./bin/stop-temporal-services.sh
```


## Temporal docker-compose.yml

```
version: "3.5"
services:
  elasticsearch:
    container_name: temporal-elasticsearch
    environment:
      - cluster.routing.allocation.disk.threshold_enabled=true
      - cluster.routing.allocation.disk.watermark.low=512mb
      - cluster.routing.allocation.disk.watermark.high=256mb
      - cluster.routing.allocation.disk.watermark.flood_stage=128mb
      - discovery.type=single-node
      - ES_JAVA_OPTS=-Xms256m -Xmx256m
      - xpack.security.enabled=false
    image: elasticsearch:${ELASTICSEARCH_VERSION}
    networks:
      - temporal-network
    expose:
      - 9200
    volumes:
      - /var/lib/elasticsearch/data
  postgresql:
    container_name: temporal-postgresql
    environment:
      POSTGRES_PASSWORD: temporal
      POSTGRES_USER: temporal
    image: postgres:${POSTGRESQL_VERSION}
    networks:
      - temporal-network
    expose:
      - 5432
    ports:
      - 5432:5432
    volumes:
      - /var/lib/postgresql/data

  php-worker:
    build:
      context: ../../..
      dockerfile: deploy/php/Dockerfile
    volumes:
      - ../../..:/app
    depends_on:
      - temporal
    networks:
      - temporal-network
    command: tail -f /dev/null
    tty: true
    stdin_open: true

  temporal:
    container_name: temporal
    depends_on:
      - postgresql
      - elasticsearch
    environment:
      - DB=postgres12
      - DB_PORT=5432
      - POSTGRES_USER=temporal
      - POSTGRES_PWD=temporal
      - POSTGRES_SEEDS=postgresql
      - DYNAMIC_CONFIG_FILE_PATH=config/dynamicconfig/development-sql.yaml
      - ENABLE_ES=true
      - ES_SEEDS=elasticsearch
      - ES_VERSION=v7
    image: temporalio/auto-setup:${TEMPORAL_VERSION}
    networks:
      - temporal-network
    ports:
      - 7233:7233
    volumes:
      - ./dynamicconfig:/etc/temporal/config/dynamicconfig
  temporal-admin-tools:
    container_name: temporal-admin-tools
    depends_on:
      - temporal
    environment:
      - TEMPORAL_ADDRESS=temporal:7233
      - TEMPORAL_CLI_ADDRESS=temporal:7233
    image: temporalio/admin-tools:${TEMPORAL_ADMINTOOLS_VERSION}
    networks:
      - temporal-network
    stdin_open: true
    tty: true
  temporal-ui:
    container_name: temporal-ui
    depends_on:
      - temporal
    environment:
      - TEMPORAL_ADDRESS=temporal:7233
      - TEMPORAL_CORS_ORIGINS=http://localhost:3000
    image: temporalio/ui:${TEMPORAL_UI_VERSION}
    networks:
      - temporal-network
    ports:
      - 8081:8080
networks:
  temporal-network:
    driver: bridge
    name: temporal-network

volumes:
  vendor:
```