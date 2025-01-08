# Temporal.io Simulation

## Install prerequisites

```
$ brew install coreutils
```

## Pulsar Manager setup

1. Start pulsar manager in docker container

```
$ docker run -it \
  -p 9527:9527 -p 7750:7750 \
  -e SPRING_CONFIGURATION_FILE=/pulsar-manager/pulsar-manager/application.properties \
  --network docker_pulsar \
  apachepulsar/pulsar-manager:latest
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

4. Create a new environment
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

1. Build the PHP worker base image

```
$ ./bin/build-php-worker.sh
```

2. Start the temporal services in docker

```
$ ./bin/start-temporal-services.sh
```

3. Log in to temporal web UI

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