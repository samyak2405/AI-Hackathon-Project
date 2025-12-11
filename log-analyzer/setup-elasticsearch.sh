#!/bin/bash

# Elasticsearch Docker Setup Script
# This script creates and runs an Elasticsearch Docker container for Log Analyser

set -e  # Exit on error

# ==============================================================================
# CONFIGURATION PROPERTIES
# ==============================================================================

# Docker container settings
CONTAINER_NAME="elasticsearch-loganalyser"
ELASTICSEARCH_IMAGE="docker.elastic.co/elasticsearch/elasticsearch:8.11.0"
ELASTICSEARCH_VERSION="8.11.0"

# Network settings
NETWORK_NAME="loganalyser-network"

# Port settings
ELASTICSEARCH_HTTP_PORT=9200
ELASTICSEARCH_TRANSPORT_PORT=9300

# Memory settings (adjust based on available system memory)
ES_JAVA_OPTS="-Xms512m -Xmx512m"

# Elasticsearch configuration
CLUSTER_NAME="loganalyser-cluster"
DISCOVERY_TYPE="single-node"
SECURITY_ENABLED="false"

# Data persistence
DATA_DIR="./elasticsearch-data"
CREATE_DATA_DIR="true"

# Health check settings
HEALTH_CHECK_RETRIES=30
HEALTH_CHECK_INTERVAL=5

# ==============================================================================
# FUNCTIONS
# ==============================================================================

print_header() {
    echo "=============================================================================="
    echo "$1"
    echo "=============================================================================="
    echo ""
}

print_info() {
    echo "[INFO] $1"
}

print_success() {
    echo "[SUCCESS] $1"
}

print_error() {
    echo "[ERROR] $1" >&2
}

print_warning() {
    echo "[WARNING] $1"
}

check_docker() {
    # Find docker binary in common locations
    DOCKER_CMD=""
    
    # Try command -v first
    if command -v docker &> /dev/null 2>&1; then
        DOCKER_CMD="docker"
    # Try common paths
    elif [ -x "/usr/local/bin/docker" ]; then
        DOCKER_CMD="/usr/local/bin/docker"
        print_info "Found Docker at /usr/local/bin/docker"
    elif [ -x "/usr/bin/docker" ]; then
        DOCKER_CMD="/usr/bin/docker"
        print_info "Found Docker at /usr/bin/docker"
    elif [ -x "/snap/bin/docker" ]; then
        DOCKER_CMD="/snap/bin/docker"
        print_info "Found Docker at /snap/bin/docker"
    else
        print_error "Docker is not installed or not found in PATH."
        print_info "Please install Docker or ensure it's in your PATH."
        exit 1
    fi
    
    # Check if docker daemon is accessible
    if ! $DOCKER_CMD info &> /dev/null 2>&1; then
        print_error "Docker daemon is not running or not accessible."
        print_info "Please start Docker first."
        exit 1
    fi
    
    # Export DOCKER_CMD for use in other functions
    export DOCKER_CMD
    
    print_success "Docker is installed and running"
    $DOCKER_CMD --version 2>/dev/null || print_info "Docker version check skipped"
}

check_port() {
    local port=$1
    # Try multiple methods to check if port is in use
    if command -v lsof &> /dev/null; then
        if lsof -Pi :$port -sTCP:LISTEN -t >/dev/null 2>&1 ; then
            return 1  # Port is in use
        fi
    elif command -v netstat &> /dev/null; then
        if netstat -tuln 2>/dev/null | grep -q ":$port " ; then
            return 1  # Port is in use
        fi
    elif command -v ss &> /dev/null; then
        if ss -tuln 2>/dev/null | grep -q ":$port " ; then
            return 1  # Port is in use
        fi
    else
        # If no port checking tool available, try to connect
        if timeout 1 bash -c "echo >/dev/tcp/localhost/$port" 2>/dev/null; then
            return 1  # Port is in use
        fi
    fi
    return 0  # Port is available (or we can't determine)
}

create_network() {
    if ! $DOCKER_CMD network ls | grep -q "$NETWORK_NAME"; then
        print_info "Creating Docker network: $NETWORK_NAME"
        $DOCKER_CMD network create "$NETWORK_NAME"
        print_success "Network created: $NETWORK_NAME"
    else
        print_info "Network already exists: $NETWORK_NAME"
    fi
}

create_data_directory() {
    if [ "$CREATE_DATA_DIR" = "true" ]; then
        if [ ! -d "$DATA_DIR" ]; then
            print_info "Creating data directory: $DATA_DIR"
            mkdir -p "$DATA_DIR"
            chmod 777 "$DATA_DIR"
            print_success "Data directory created: $DATA_DIR"
        else
            print_info "Data directory already exists: $DATA_DIR"
        fi
    fi
}

stop_existing_container() {
    # Check for container with same name
    if $DOCKER_CMD ps -a --format '{{.Names}}' | grep -q "^${CONTAINER_NAME}$"; then
        print_warning "Container '$CONTAINER_NAME' already exists"
        read -p "Do you want to stop and remove it? (y/n) " -n 1 -r
        echo
        if [[ $REPLY =~ ^[Yy]$ ]]; then
            print_info "Stopping existing container..."
            $DOCKER_CMD stop "$CONTAINER_NAME" 2>/dev/null || true
            print_info "Removing existing container..."
            $DOCKER_CMD rm "$CONTAINER_NAME" 2>/dev/null || true
            print_success "Existing container removed"
        else
            print_info "Keeping existing container. Exiting..."
            exit 0
        fi
    fi
}

check_ports() {
    # Check if ports are in use
    local http_in_use=false
    local transport_in_use=false
    local original_http_port=$ELASTICSEARCH_HTTP_PORT
    local original_transport_port=$ELASTICSEARCH_TRANSPORT_PORT
    
    if ! check_port $ELASTICSEARCH_HTTP_PORT; then
        http_in_use=true
        print_warning "Port $ELASTICSEARCH_HTTP_PORT is already in use"
        
        # Check if it's an existing Elasticsearch container
        local existing_container=$($DOCKER_CMD ps --format '{{.Names}} {{.Ports}}' | grep ":${ELASTICSEARCH_HTTP_PORT}->" | awk '{print $1}' | head -1)
        if [ -n "$existing_container" ]; then
            print_info "Found existing Elasticsearch container: $existing_container"
            echo ""
            print_info "Options:"
            echo "  1. Use existing Elasticsearch container (recommended)"
            echo "  2. Stop existing container and create new one"
            echo "  3. Use different ports for new container"
            echo "  4. Exit"
            read -p "Choose an option (1-4): " option
            echo
            
            case $option in
                1)
                    print_info "You chose to use existing container: $existing_container"
                    print_info "Update your application.properties with:"
                    echo "  elasticsearch.host=localhost"
                    echo "  elasticsearch.port=$ELASTICSEARCH_HTTP_PORT"
                    exit 0
                    ;;
                2)
                    print_info "Stopping existing container: $existing_container"
                    $DOCKER_CMD stop "$existing_container" 2>/dev/null || true
                    sleep 2
                    if check_port $ELASTICSEARCH_HTTP_PORT; then
                        print_success "Port $ELASTICSEARCH_HTTP_PORT is now available"
                        http_in_use=false
                    else
                        print_error "Failed to free port $ELASTICSEARCH_HTTP_PORT"
                        exit 1
                    fi
                    ;;
                3)
                    # Find available ports
                    print_info "Finding available ports..."
                    local new_http_port=$ELASTICSEARCH_HTTP_PORT
                    local new_transport_port=$ELASTICSEARCH_TRANSPORT_PORT
                    local port_found=false
                    
                    for i in {1..100}; do
                        new_http_port=$((ELASTICSEARCH_HTTP_PORT + i))
                        new_transport_port=$((ELASTICSEARCH_TRANSPORT_PORT + i))
                        
                        if check_port $new_http_port && check_port $new_transport_port; then
                            ELASTICSEARCH_HTTP_PORT=$new_http_port
                            ELASTICSEARCH_TRANSPORT_PORT=$new_transport_port
                            port_found=true
                            print_success "Found available ports: HTTP=$new_http_port, Transport=$new_transport_port"
                            print_warning "Update your application.properties:"
                            echo "  elasticsearch.host=localhost"
                            echo "  elasticsearch.port=$new_http_port"
                            break
                        fi
                    done
                    
                    if [ "$port_found" = "false" ]; then
                        print_error "Could not find available ports. Please free up ports manually."
                        exit 1
                    fi
                    http_in_use=false
                    transport_in_use=false
                    ;;
                4)
                    print_info "Exiting..."
                    exit 0
                    ;;
                *)
                    print_error "Invalid option. Exiting..."
                    exit 1
                    ;;
            esac
        else
            # Port is in use but not by an Elasticsearch container
            print_info "Port is in use by another service"
            read -p "Continue with different ports? (y/n) " -n 1 -r
            echo
            if [[ $REPLY =~ ^[Yy]$ ]]; then
                # Try to find alternative ports
                for i in {1..100}; do
                    local test_http=$((original_http_port + i))
                    local test_transport=$((original_transport_port + i))
                    if check_port $test_http && check_port $test_transport; then
                        ELASTICSEARCH_HTTP_PORT=$test_http
                        ELASTICSEARCH_TRANSPORT_PORT=$test_transport
                        print_info "Using alternative ports: HTTP=$test_http, Transport=$test_transport"
                        print_warning "Update your application.properties:"
                        echo "  elasticsearch.host=localhost"
                        echo "  elasticsearch.port=$test_http"
                        http_in_use=false
                        transport_in_use=false
                        break
                    fi
                done
            else
                exit 0
            fi
        fi
    fi
    
    if ! check_port $ELASTICSEARCH_TRANSPORT_PORT; then
        transport_in_use=true
        print_warning "Port $ELASTICSEARCH_TRANSPORT_PORT is already in use"
        
        if [ "$http_in_use" = "false" ]; then
            # HTTP port is free, but transport is not, try to find alternative
            for i in {1..100}; do
                local test_transport=$((ELASTICSEARCH_TRANSPORT_PORT + i))
                if check_port $test_transport; then
                    ELASTICSEARCH_TRANSPORT_PORT=$test_transport
                    print_info "Using alternative transport port: $test_transport"
                    transport_in_use=false
                    break
                fi
            done
        fi
    fi
    
    if [ "$http_in_use" = "false" ] && [ "$transport_in_use" = "false" ]; then
        if [ "$ELASTICSEARCH_HTTP_PORT" != "$original_http_port" ] || [ "$ELASTICSEARCH_TRANSPORT_PORT" != "$original_transport_port" ]; then
            print_info "Using ports: HTTP=$ELASTICSEARCH_HTTP_PORT, Transport=$ELASTICSEARCH_TRANSPORT_PORT"
        else
            print_success "Ports are available"
        fi
    else
        print_error "Could not resolve port conflicts. Please free up ports or configure different ports."
        exit 1
    fi
}

pull_image() {
    print_info "Pulling Elasticsearch image: $ELASTICSEARCH_IMAGE"
    $DOCKER_CMD pull "$ELASTICSEARCH_IMAGE"
    print_success "Image pulled successfully"
}

run_container() {
    print_info "Starting Elasticsearch container..."
    
    local docker_cmd="$DOCKER_CMD run -d \
        --name $CONTAINER_NAME \
        --network $NETWORK_NAME \
        -p $ELASTICSEARCH_HTTP_PORT:9200 \
        -p $ELASTICSEARCH_TRANSPORT_PORT:9300 \
        -e \"discovery.type=$DISCOVERY_TYPE\" \
        -e \"cluster.name=$CLUSTER_NAME\" \
        -e \"ES_JAVA_OPTS=$ES_JAVA_OPTS\" \
        -e \"xpack.security.enabled=$SECURITY_ENABLED\" \
        -e \"xpack.security.enrollment.enabled=false\" \
        -e \"xpack.security.http.ssl.enabled=false\" \
        -e \"xpack.security.transport.ssl.enabled=false\""
    
    # Add data volume if data directory is enabled
    if [ "$CREATE_DATA_DIR" = "true" ]; then
        docker_cmd="$docker_cmd -v $(pwd)/$DATA_DIR:/usr/share/elasticsearch/data"
    fi
    
    docker_cmd="$docker_cmd $ELASTICSEARCH_IMAGE"
    
    print_info "Executing container creation..."
    eval $docker_cmd
    
    if [ $? -eq 0 ]; then
        print_success "Container started: $CONTAINER_NAME"
    else
        print_error "Failed to start container"
        exit 1
    fi
}

wait_for_elasticsearch() {
    print_info "Waiting for Elasticsearch to be ready (this may take a minute)..."
    
    local retries=0
    local max_retries=$HEALTH_CHECK_RETRIES
    local interval=$HEALTH_CHECK_INTERVAL
    local url="http://localhost:$ELASTICSEARCH_HTTP_PORT"
    
    while [ $retries -lt $max_retries ]; do
        if curl -s "$url" > /dev/null 2>&1; then
            print_success "Elasticsearch is ready!"
            return 0
        fi
        
        retries=$((retries + 1))
        echo -n "."
        sleep $interval
    done
    
    echo ""
    print_error "Elasticsearch did not become ready within expected time"
    print_info "Check container logs: docker logs $CONTAINER_NAME"
    return 1
}

verify_elasticsearch() {
    print_info "Verifying Elasticsearch connection..."
    
    local url="http://localhost:$ELASTICSEARCH_HTTP_PORT"
    local response=$(curl -s "$url")
    
    if [ -n "$response" ]; then
        print_success "Elasticsearch is responding"
        echo ""
        echo "Cluster Information:"
        echo "$response" | python3 -m json.tool 2>/dev/null || echo "$response"
    else
        print_error "Elasticsearch is not responding"
        return 1
    fi
}

print_configuration() {
    print_header "Configuration Summary"
    echo "Container Name:      $CONTAINER_NAME"
    echo "Image:               $ELASTICSEARCH_IMAGE"
    echo "HTTP Port:           $ELASTICSEARCH_HTTP_PORT"
    echo "Transport Port:      $ELASTICSEARCH_TRANSPORT_PORT"
    echo "Cluster Name:        $CLUSTER_NAME"
    echo "Discovery Type:      $DISCOVERY_TYPE"
    echo "Security Enabled:    $SECURITY_ENABLED"
    echo "Memory (JVM):        $ES_JAVA_OPTS"
    echo "Data Directory:      $DATA_DIR"
    echo "Network:             $NETWORK_NAME"
    echo ""
}

print_connection_info() {
    print_header "Connection Information"
    echo "Elasticsearch URL:   http://localhost:$ELASTICSEARCH_HTTP_PORT"
    echo "Cluster Name:        $CLUSTER_NAME"
    echo ""
    echo "To verify Elasticsearch is running:"
    echo "  curl http://localhost:$ELASTICSEARCH_HTTP_PORT"
    echo ""
    echo "To view container logs:"
    echo "  docker logs $CONTAINER_NAME"
    echo ""
    echo "To stop the container:"
    echo "  docker stop $CONTAINER_NAME"
    echo ""
    echo "To start the container again:"
    echo "  docker start $CONTAINER_NAME"
    echo ""
    echo "To remove the container:"
    echo "  docker rm -f $CONTAINER_NAME"
    echo ""
}

# ==============================================================================
# MAIN EXECUTION
# ==============================================================================

main() {
    print_header "Elasticsearch Docker Setup for Log Analyser"
    
    print_configuration
    
    # Pre-flight checks
    print_header "Pre-flight Checks"
    check_docker
    check_ports
    
    # Setup
    print_header "Setup"
    create_network
    create_data_directory
    stop_existing_container
    
    # Pull and run
    print_header "Container Setup"
    pull_image
    run_container
    
    # Wait and verify
    print_header "Health Check"
    if wait_for_elasticsearch; then
        verify_elasticsearch
        
        print_header "Setup Complete!"
        print_connection_info
        
        print_success "Elasticsearch is ready to use!"
        print_info "Update your application.properties with:"
        echo "  elasticsearch.host=localhost"
        echo "  elasticsearch.port=$ELASTICSEARCH_HTTP_PORT"
    else
        print_error "Setup failed. Please check the logs."
        exit 1
    fi
}

# Run main function
main "$@"

