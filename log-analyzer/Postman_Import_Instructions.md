# Postman Collection Import Instructions

## How to Import the Collection

1. **Open Postman**
   - Launch the Postman application

2. **Import Collection**
   - Click on "Import" button (top left)
   - Or use `Ctrl+O` (Windows/Linux) or `Cmd+O` (Mac)

3. **Select File**
   - Click "Upload Files"
   - Navigate to `LogAnalyser.postman_collection.json`
   - Select the file and click "Import"

4. **Verify Import**
   - The collection "Log Analyser API" should appear in your Postman sidebar
   - Expand it to see all the requests

## Collection Contents

The collection includes the following requests:

### 1. Health Check
- **Method**: GET
- **Endpoint**: `/api/health`
- **Purpose**: Verify the service is running

### 2. Query - Transaction Analysis
- **Method**: POST
- **Endpoint**: `/api/query`
- **Transaction ID**: `TX872000310`
- **Purpose**: General transaction analysis

### 3. Query - Transaction Status
- **Method**: POST
- **Endpoint**: `/api/query`
- **Transaction ID**: `TX651750504`
- **Purpose**: Check transaction success/failure status

### 4. Query - Error Analysis
- **Method**: POST
- **Endpoint**: `/api/query`
- **Transaction ID**: `TX872000310`
- **Purpose**: Analyze errors in transaction logs

### 5. Query - Transaction Timeline
- **Method**: POST
- **Endpoint**: `/api/query`
- **Transaction ID**: `TX820325700`
- **Purpose**: Get chronological event timeline

### 6. Query - Payment Details
- **Method**: POST
- **Endpoint**: `/api/query`
- **Transaction ID**: `TX651750504`
- **Purpose**: Extract payment information

### 7. Query - Non-existent Transaction
- **Method**: POST
- **Endpoint**: `/api/query`
- **Transaction ID**: `TX999999999`
- **Purpose**: Test error handling

### 8. Query - Missing Transaction ID
- **Method**: POST
- **Endpoint**: `/api/query`
- **Purpose**: Test validation errors

### 9. Query - Missing Query
- **Method**: POST
- **Endpoint**: `/api/query`
- **Purpose**: Test validation errors

## Environment Variables

The collection uses a variable `base_url` which is set to `http://localhost:8080` by default.

### To Change the Base URL:

1. Click on the collection name
2. Go to the "Variables" tab
3. Edit the `base_url` value
4. Click "Save"

### Available Transaction IDs (from database):

- `TX872000310`
- `TX695163134`
- `TX651750504`
- `TX537465762`
- `TX820325700`
- `TX392060453`
- And 58 more...

## Running Tests

1. **Start the Application**
   ```bash
   mvn spring-boot:run
   ```

2. **Run Requests in Postman**
   - Select any request from the collection
   - Click "Send"
   - View the response

3. **Run Collection**
   - Click on the collection name
   - Click "Run" button
   - Select which requests to run
   - Click "Run Log Analyser API"

## Expected Responses

### Success Response (200 OK)
```json
{
  "response": "Based on the logs, ..."
}
```

### Error Response (500 Internal Server Error)
```json
{
  "response": "Error: Transaction ID 'TX999999999' not found in database."
}
```

### Validation Error (400 Bad Request)
```json
{
  "timestamp": "2024-12-06T...",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "errors": [...]
}
```

## Tips

- Make sure the application is running before testing
- Check the console logs for detailed error messages
- The OpenAI API key must be set in environment variables
- Transaction IDs must exist in the database (loaded via Liquibase)

