# Elasticsearch Integration Guide

## Overview

The Log Analyser application now uses Elasticsearch to store and retrieve logs with pagination support. All logs from the log file are indexed into Elasticsearch, and queries fetch logs in paginated form before sending them to OpenAI for analysis.

## Architecture

1. **Log Indexing**: On application startup, logs are automatically indexed from the log file into Elasticsearch
2. **Log Retrieval**: When querying, logs are fetched from Elasticsearch in paginated batches
3. **OpenAI Analysis**: All retrieved logs are sent to OpenAI for intelligent analysis

## Setup

### 1. Install and Run Elasticsearch

#### Using Docker (Recommended):
```bash
docker run -d \
  --name elasticsearch \
  -p 9200:9200 \
  -p 9300:9300 \
  -e "discovery.type=single-node" \
  -e "xpack.security.enabled=false" \
  -e "ES_JAVA_OPTS=-Xms512m -Xmx512m" \
  docker.elastic.co/elasticsearch/elasticsearch:8.11.0
```

#### Using Homebrew (Mac):
```bash
brew tap elastic/tap
brew install elastic/tap/elasticsearch-full
brew services start elastic/tap/elasticsearch-full
```

#### Using Package Manager (Linux):
```bash
# Ubuntu/Debian
wget https://artifacts.elastic.co/downloads/elasticsearch/elasticsearch-8.11.0-amd64.deb
sudo dpkg -i elasticsearch-8.11.0-amd64.deb
sudo systemctl start elasticsearch
```

### 2. Verify Elasticsearch is Running

```bash
curl http://localhost:9200
```

You should see a JSON response with cluster information.

### 3. Configuration

The application is configured to connect to Elasticsearch via `application.properties`:

```properties
# Elasticsearch Configuration
elasticsearch.host=localhost
elasticsearch.port=9200
elasticsearch.scheme=http
elasticsearch.index.on.startup=true
elasticsearch.page.size=100
```

### 4. Environment Variables (Optional)

You can override configuration using environment variables:

```bash
export ELASTICSEARCH_HOST=localhost
export ELASTICSEARCH_PORT=9200
export ELASTICSEARCH_SCHEME=http
export ELASTICSEARCH_INDEX_ON_STARTUP=true
export ELASTICSEARCH_PAGE_SIZE=100
```

## How It Works

### Indexing Process

1. On application startup, `LogIndexingService` automatically reads the log file
2. Each log line is parsed to extract:
   - Transaction ID
   - UUID
   - Client Transaction ID
   - User ID
   - Log Level (INFO, DEBUG, ERROR, etc.)
   - Service name
   - Timestamp
   - Line number
3. Logs are indexed in batches of 100 to Elasticsearch

### Query Process

1. User sends a query with a transaction ID
2. Application looks up transaction in H2 database to get UUID
3. Application searches Elasticsearch for logs matching the transaction ID
4. Logs are fetched in paginated batches (default: 100 per page)
5. All logs are collected and sent to OpenAI for analysis
6. OpenAI response is returned to the user

## Features

### Pagination

- Logs are retrieved in configurable page sizes (default: 100)
- Automatic pagination when fetching all logs for a transaction
- Efficient memory usage for large log sets

### Search Capabilities

- Search by Transaction ID
- Search by UUID
- Search by Transaction ID or UUID
- Full-text search on log content

### Automatic Indexing

- Logs are automatically indexed on application startup
- Can be disabled by setting `elasticsearch.index.on.startup=false`

## API Usage

The API remains the same, but now uses Elasticsearch:

```bash
curl -X POST http://localhost:8080/api/query \
  -H "Content-Type: application/json" \
  -d '{
    "transactionId": "TX872000310",
    "query": "What happened with this transaction?"
  }'
```

## Monitoring

### Check Index Status

```bash
# List all indices
curl http://localhost:9200/_cat/indices?v

# Check document count
curl http://localhost:9200/application-logs/_count

# Search logs manually
curl -X GET "http://localhost:9200/application-logs/_search?q=transaction_id:TX872000310"
```

### View Indexed Documents

```bash
curl -X GET "http://localhost:9200/application-logs/_search?pretty" \
  -H "Content-Type: application/json" \
  -d '{
    "query": {
      "match": {
        "transaction_id": "TX872000310"
      }
    },
    "size": 10
  }'
```

## Troubleshooting

### Elasticsearch Connection Issues

1. **Check if Elasticsearch is running:**
   ```bash
   curl http://localhost:9200
   ```

2. **Check application logs** for connection errors

3. **Verify configuration** in `application.properties`

### Indexing Issues

1. **Check if indexing is enabled:**
   - Verify `elasticsearch.index.on.startup=true` in properties

2. **Check log file path:**
   - Ensure `log.file.path` points to a valid log file

3. **Check Elasticsearch logs:**
   ```bash
   # Docker
   docker logs elasticsearch
   
   # Systemd
   journalctl -u elasticsearch
   ```

### Performance Tuning

1. **Adjust page size** if you have memory constraints:
   ```properties
   elasticsearch.page.size=50  # Smaller batches
   ```

2. **Disable indexing on startup** if you want to index manually:
   ```properties
   elasticsearch.index.on.startup=false
   ```

3. **Reindex logs** if needed (restart application with indexing enabled)

## Manual Operations

### Reindex Logs

Restart the application or call the indexing service programmatically.

### Clear Index (Development Only)

```bash
# Delete the index
curl -X DELETE http://localhost:9200/application-logs

# Restart application to reindex
```

### Search Logs Directly

```bash
# Search by transaction ID
curl -X GET "http://localhost:9200/application-logs/_search?pretty" \
  -H "Content-Type: application/json" \
  -d '{
    "query": {
      "term": {
        "transaction_id": "TX872000310"
      }
    }
  }'
```

## Benefits

1. **Scalability**: Can handle large log files efficiently
2. **Performance**: Fast search and retrieval with pagination
3. **Flexibility**: Easy to query and filter logs
4. **Separation**: Log storage separate from application logic
5. **Search**: Full-text search capabilities
6. **Analytics**: Can perform complex queries on logs

## Production Considerations

1. **Security**: Enable authentication for Elasticsearch in production
2. **Clustering**: Use Elasticsearch cluster for high availability
3. **Backup**: Implement regular index snapshots
4. **Retention**: Set up index lifecycle management for log retention
5. **Monitoring**: Use Elasticsearch monitoring tools
6. **Index Templates**: Create index templates for consistent mapping

