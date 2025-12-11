# Log Analyser

A Spring Boot application that analyzes log files using OpenAI. The application extracts transaction IDs from queries, searches log files, and uses OpenAI to provide intelligent analysis.

## Features

- REST API endpoint to submit queries
- H2 database with transaction ID to UUID mapping
- Automatic transaction ID lookup from database
- Log file grepping based on transaction ID
- OpenAI integration for intelligent log analysis

## Prerequisites

- Java 17 or higher
- Maven 3.6+
- OpenAI API key

## Configuration

1. Set your OpenAI API key as an environment variable:
   ```bash
   export OPENAI_API_KEY=your-api-key-here
   ```

2. Configure the log file path (optional, defaults to `./logs/application.log`):
   ```bash
   export LOG_FILE_PATH=/path/to/your/logfile.log
   ```

   Or update `src/main/resources/application.properties`:
   ```properties
   openai.api.key=your-api-key-here
   log.file.path=/path/to/your/logfile.log
   ```

## Building and Running

1. Build the project:
   ```bash
   mvn clean install
   ```

2. Run the application:
   ```bash
   mvn spring-boot:run
   ```

   Or run the JAR:
   ```bash
   java -jar target/loganalyser-1.0.0.jar
   ```

The application will start on port 8080 by default.

## API Usage

### Health Check

```bash
curl -X GET http://localhost:8080/api/health
```

### Query Endpoint

Submit a query with a transaction ID to analyze logs:

```bash
curl -X POST http://localhost:8080/api/query \
  -H "Content-Type: application/json" \
  -d '{
    "transactionId": "TX872000310",
    "query": "What happened with this transaction? Analyze the logs and provide details."
  }'
```

**Request Body:**
```json
{
  "transactionId": "TX872000310",
  "query": "Your question about the transaction"
}
```

**Response:**
```json
{
  "response": "Based on the logs, ..."
}
```

### Example Queries

**Example 1: Check transaction status**
```bash
curl -X POST http://localhost:8080/api/query \
  -H "Content-Type: application/json" \
  -d '{
    "transactionId": "TX651750504",
    "query": "What was the status of this transaction? Was it successful or failed?"
  }'
```

**Example 2: Analyze errors**
```bash
curl -X POST http://localhost:8080/api/query \
  -H "Content-Type: application/json" \
  -d '{
    "transactionId": "TX872000310",
    "query": "What errors occurred during this transaction? Provide details about any failures."
  }'
```

**Example 3: Get transaction timeline**
```bash
curl -X POST http://localhost:8080/api/query \
  -H "Content-Type: application/json" \
  -d '{
    "transactionId": "TX820325700",
    "query": "What is the timeline of events for this transaction? List all steps in chronological order."
  }'
```

### Running Test Script

A test script is provided to test all endpoints:

```bash
./test-api.sh
```

Or manually:

```bash
bash test-api.sh
```

## How It Works

1. **Query Submission**: The API receives a transaction ID and query string
2. **Database Lookup**: The service searches the H2 database for the transaction ID
3. **UUID Retrieval**: If found, retrieves the UUID and Service ID corresponding to the transaction ID
4. **Log Grepping**: The service searches the log file for all lines containing the transaction ID
5. **OpenAI Analysis**: The query and relevant logs are sent to OpenAI for analysis
6. **Response**: The AI-generated analysis is returned to the client

## Database

The application uses H2 in-memory database with Liquibase for schema management. The database is initialized with 64 transaction records extracted from the log file.

### Accessing H2 Console

1. Start the application
2. Navigate to: http://localhost:8080/h2-console
3. Use these connection details:
   - JDBC URL: `jdbc:h2:mem:loganalyser`
   - Username: `sa`
   - Password: (leave empty)

## Transaction ID Format

The application recognizes transaction IDs in the format:
- `TX872000310`
- `TX651750504`
- etc.

All transaction IDs are stored in the database with their corresponding UUIDs and Service IDs.

## Project Structure

```
src/
├── main/
│   ├── java/com/loganalyser/
│   │   ├── LogAnalyserApplication.java    # Main application class
│   │   ├── config/
│   │   │   └── OpenAIConfig.java          # OpenAI configuration
│   │   ├── controller/
│   │   │   └── QueryController.java       # REST API endpoints
│   │   ├── entity/
│   │   │   └── Transaction.java           # Transaction entity
│   │   ├── repository/
│   │   │   └── TransactionRepository.java # JPA repository
│   │   └── service/
│   │       ├── LogAnalysisService.java    # Main orchestration service
│   │       ├── TransactionIdExtractorService.java  # Transaction ID extraction
│   │       ├── LogFileService.java        # Log file grepping
│   │       └── OpenAIService.java         # OpenAI integration
│   └── resources/
│       ├── application.properties         # Configuration
│       └── db/changelog/
│           └── db.changelog-master.xml    # Liquibase changeset
```

## Troubleshooting

- **No logs found**: Ensure the log file path is correct and the file is readable
- **OpenAI API errors**: Verify your API key is set correctly and you have sufficient credits
- **Transaction ID not found**: Check if the transaction ID exists in the database. You can query the H2 console to verify.
- **Database initialization issues**: Check Liquibase logs in the application startup output

## License

This project is provided as-is for demonstration purposes.



Restart command
cd /Users/apple/Documents/Projects/Hackathon-app/db-llm-analyzer
source venv/bin/activate
DB_USER=postgres DB_PASSWORD=1234 DB_HOST=localhost DB_PORT=5432 DB_NAME=postgres API_PORT=8083 uvicorn api:app --host 0.0.0.0 --port 8083