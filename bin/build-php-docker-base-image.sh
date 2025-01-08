#!/bin/bash

# Enable BuildKit
export DOCKER_BUILDKIT=1

cd deploy/php || exit

# Build with cache mounting and export cache
docker compose build \
    --build-arg BUILDKIT_INLINE_CACHE=1 \
    --progress=plain

# The --progress=plain flag will show detailed build output
