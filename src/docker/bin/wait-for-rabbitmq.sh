#!/usr/bin/env bash

# Wait for the rabbitmq and postgres container to be ready before starting java process.

wait-for-it.sh -h $RABBITMQ_HOST -p $RABBITMQ_PORT -t 0

echo "Starting Application"
exec "$@"