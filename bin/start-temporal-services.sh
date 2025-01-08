#!/bin/bash

# Build the php-worker image with cache
docker compose -f deploy/temporal/docker-compose/docker-compose.yml build \
    --build-arg BUILDKIT_INLINE_CACHE=1 \
    php-worker

# Start the services
docker compose -f deploy/temporal/docker-compose/docker-compose.yml up -d
