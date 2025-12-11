#!/bin/bash

# Stop and Remove Elasticsearch Docker Container

set -e

CONTAINER_NAME="elasticsearch-loganalyser"
NETWORK_NAME="loganalyser-network"
DATA_DIR="./elasticsearch-data"

echo "=============================================================================="
echo "Stopping Elasticsearch Container"
echo "=============================================================================="
echo ""

if docker ps --format '{{.Names}}' | grep -q "^${CONTAINER_NAME}$"; then
    echo "[INFO] Stopping container: $CONTAINER_NAME"
    docker stop "$CONTAINER_NAME"
    echo "[SUCCESS] Container stopped"
else
    echo "[INFO] Container is not running: $CONTAINER_NAME"
fi

read -p "Do you want to remove the container? (y/n) " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    if docker ps -a --format '{{.Names}}' | grep -q "^${CONTAINER_NAME}$"; then
        echo "[INFO] Removing container: $CONTAINER_NAME"
        docker rm "$CONTAINER_NAME"
        echo "[SUCCESS] Container removed"
    else
        echo "[INFO] Container does not exist: $CONTAINER_NAME"
    fi
fi

read -p "Do you want to remove the Docker network? (y/n) " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    if docker network ls | grep -q "$NETWORK_NAME"; then
        echo "[INFO] Removing network: $NETWORK_NAME"
        docker network rm "$NETWORK_NAME"
        echo "[SUCCESS] Network removed"
    else
        echo "[INFO] Network does not exist: $NETWORK_NAME"
    fi
fi

read -p "Do you want to remove the data directory ($DATA_DIR)? (y/n) " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    if [ -d "$DATA_DIR" ]; then
        echo "[INFO] Removing data directory: $DATA_DIR"
        rm -rf "$DATA_DIR"
        echo "[SUCCESS] Data directory removed"
    else
        echo "[INFO] Data directory does not exist: $DATA_DIR"
    fi
fi

echo ""
echo "[SUCCESS] Cleanup complete!"

