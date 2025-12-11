# Elasticsearch Docker Setup Scripts

This directory contains shell scripts to easily set up and manage Elasticsearch using Docker for the Log Analyser application.

## Scripts

### 1. `setup-elasticsearch.sh`
Main script to create and run Elasticsearch Docker container with all configurations.

### 2. `stop-elasticsearch.sh`
Helper script to stop and clean up Elasticsearch container and resources.

## Quick Start

### Setup Elasticsearch

```bash
./setup-elasticsearch.sh
```

This script will:
- Check Docker installation and availability
- Check if required ports are available
- Create Docker network if needed
- Create data directory for persistence
- Pull Elasticsearch Docker image
- Run Elasticsearch container with configured properties
- Wait for Elasticsearch to be ready
- Verify the connection

### Stop Elasticsearch

```bash
./stop-elasticsearch.sh
```

This script will:
- Stop the running container
- Optionally remove the container
- Optionally remove the Docker network
- Optionally remove the data directory

## Configuration Properties

All configuration is defined in `setup-elasticsearch.sh`. You can modify these properties:

### Container Settings
```bash
CONTAINER_NAME="elasticsearch-loganalyser"
ELASTICSEARCH_IMAGE="docker.elastic.co/elasticsearch/elasticsearch:8.11.0"
ELASTICSEARCH_VERSION="8.11.0"
```

### Network Settings
```bash
NETWORK_NAME="loganalyser-network"
```

### Port Settings
```bash
ELASTICSEARCH_HTTP_PORT=9200
ELASTICSEARCH_TRANSPORT_PORT=9300
```

### Memory Settings
```bash
ES_JAVA_OPTS="-Xms512m -Xmx512m"
```
**Note**: Adjust based on your system's available memory. Minimum recommended is 512MB.

### Elasticsearch Configuration
```bash
CLUSTER_NAME="loganalyser-cluster"
DISCOVERY_TYPE="single-node"
SECURITY_ENABLED="false"
```

### Data Persistence
```bash
DATA_DIR="./elasticsearch-data"
CREATE_DATA_DIR="true"
```

### Health Check Settings
```bash
HEALTH_CHECK_RETRIES=30
HEALTH_CHECK_INTERVAL=5
```

## Usage Examples

### Basic Setup
```bash
./setup-elasticsearch.sh
```

### Custom Port Setup
Edit the script and change:
```bash
ELASTICSEARCH_HTTP_PORT=9201
```
Then run:
```bash
./setup-elasticsearch.sh
```

### Setup with More Memory
Edit the script and change:
```bash
ES_JAVA_OPTS="-Xms1g -Xmx1g"
```
Then run:
```bash
./setup-elasticsearch.sh
```

## Verification

After running the setup script, verify Elasticsearch is working:

```bash
# Check cluster info
curl http://localhost:9200

# Check cluster health
curl http://localhost:9200/_cluster/health?pretty

# List indices
curl http://localhost:9200/_cat/indices?v
```

## Application Configuration

After Elasticsearch is running, ensure your `application.properties` matches:

```properties
elasticsearch.host=localhost
elasticsearch.port=9200
elasticsearch.scheme=http
elasticsearch.index.on.startup=true
elasticsearch.page.size=100
```

## Troubleshooting

### Port Already in Use

If you get an error that ports are already in use:

1. Check what's using the port:
   ```bash
   lsof -i :9200
   ```

2. Stop the service using the port, or

3. Change the port in the script:
   ```bash
   ELASTICSEARCH_HTTP_PORT=9201
   ```

### Container Won't Start

Check container logs:
```bash
docker logs elasticsearch-loganalyser
```

### Elasticsearch Not Ready

If the health check fails:

1. Check container status:
   ```bash
   docker ps -a | grep elasticsearch
   ```

2. Check container logs:
   ```bash
   docker logs elasticsearch-loganalyser
   ```

3. Increase health check retries in the script if needed

### Out of Memory

If Elasticsearch fails due to memory:

1. Reduce memory allocation in the script:
   ```bash
   ES_JAVA_OPTS="-Xms256m -Xmx256m"
   ```

2. Or increase system available memory

### Data Persistence Issues

If you have permission issues with the data directory:

1. Ensure the directory has proper permissions:
   ```bash
   chmod 777 elasticsearch-data
   ```

2. Or run the script with appropriate permissions

## Docker Commands Reference

### View Container Logs
```bash
docker logs elasticsearch-loganalyser
```

### Follow Container Logs
```bash
docker logs -f elasticsearch-loganalyser
```

### Check Container Status
```bash
docker ps | grep elasticsearch
```

### Restart Container
```bash
docker restart elasticsearch-loganalyser
```

### Stop Container
```bash
docker stop elasticsearch-loganalyser
```

### Start Container
```bash
docker start elasticsearch-loganalyser
```

### Remove Container
```bash
docker rm -f elasticsearch-loganalyser
```

## Advanced Configuration

### Enable Security (Production)

For production use, enable security:

1. Edit `setup-elasticsearch.sh`:
   ```bash
   SECURITY_ENABLED="true"
   ```

2. Add password generation or configuration

3. Update application properties with credentials

### Cluster Setup

For multi-node cluster:

1. Modify `DISCOVERY_TYPE` in the script
2. Run multiple containers
3. Configure cluster settings

### Custom Elasticsearch Configuration

You can mount custom configuration files:

1. Create a `elasticsearch.yml` file
2. Modify the docker run command in the script to include:
   ```bash
   -v $(pwd)/elasticsearch.yml:/usr/share/elasticsearch/config/elasticsearch.yml
   ```

## Notes

- The script uses single-node mode by default for simplicity
- Security is disabled by default for development
- Data is persisted in `./elasticsearch-data` directory
- The script automatically handles cleanup of existing containers
- Health checks wait up to 2.5 minutes (30 retries × 5 seconds)

## Support

For issues or questions:
1. Check container logs: `docker logs elasticsearch-loganalyser`
2. Check Elasticsearch logs in the container
3. Verify Docker is running and has sufficient resources
4. Check system memory availability

