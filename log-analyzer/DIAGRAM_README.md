# Draw.io Diagrams for Log Analyser Project

This directory contains draw.io (diagrams.net) diagram files for the Log Analyser project.

## Files

### 1. `LogAnalyser-Architecture.drawio`
**System Architecture Diagram**

Shows the complete system architecture including:
- **Client Layer**: Client applications making requests
- **API Layer**: QueryController with REST endpoints
- **Service Layer**: 
  - LogAnalysisService (orchestration)
  - ElasticsearchService (log search & pagination)
  - OpenAIService (AI analysis)
  - TransactionRepository (JPA)
- **Data Layer**:
  - H2 Database (transaction metadata)
  - Elasticsearch (log storage)
  - Log File (source)
- **External Services**: OpenAI API
- **Infrastructure**: Liquibase (schema management), LogIndexingService

**Color Coding**:
- 🔵 Blue: Client/API layers
- 🟢 Green: Service orchestration
- 🟣 Purple: Database/JPA
- 🔴 Red: Elasticsearch
- 🟠 Orange: OpenAI/External APIs
- ⚪ Gray: File/Configuration

### 2. `LogAnalyser-Flow-Diagram.drawio`
**Request Flow Diagram**

Shows the step-by-step flow of a request:
1. Client sends POST request with transactionId and query
2. QueryController validates and routes
3. Lookup transaction in H2 database (get UUID)
4. Search logs in Elasticsearch (paginated)
5. Send logs to OpenAI for analysis
6. Return response to client

Also includes:
- **Startup Flow**: How logs are indexed on application startup
- **Error Handling**: Error paths for missing transactions/logs
- **Notes**: Important implementation details

## How to Open

### Option 1: Online (Recommended)
1. Go to [https://app.diagrams.net/](https://app.diagrams.net/) (formerly draw.io)
2. Click "Open Existing Diagram"
3. Select the `.drawio` file from this directory
4. The diagram will open in your browser

### Option 2: Desktop App
1. Download draw.io desktop app from [https://github.com/jgraph/drawio-desktop/releases](https://github.com/jgraph/drawio-desktop/releases)
2. Install and open the application
3. File → Open → Select the `.drawio` file

### Option 3: VS Code Extension
1. Install "Draw.io Integration" extension in VS Code
2. Open the `.drawio` file in VS Code
3. Edit directly in VS Code

## Editing the Diagrams

The diagrams are XML files that can be:
- Edited in draw.io/diagrams.net
- Version controlled (they're text-based XML)
- Exported to PNG, PDF, SVG, etc.

### Export Options
1. Open the diagram in draw.io
2. File → Export as
3. Choose format: PNG, PDF, SVG, JPEG, etc.

## Diagram Details

### Architecture Diagram Components

**Request Flow**:
```
Client → Controller → LogAnalysisService → [TransactionRepo → H2]
                                           ↓
                                    [ElasticsearchService → Elasticsearch]
                                           ↓
                                    [OpenAIService → OpenAI]
                                           ↓
                                    Response → Client
```

**Data Flow**:
- **Startup**: Log File → LogIndexingService → Elasticsearch
- **Schema**: Liquibase → H2 Database (creates tables, inserts data)
- **Query**: H2 (transaction lookup) → Elasticsearch (log search) → OpenAI (analysis)

### Flow Diagram Steps

**Normal Flow**:
1. POST /api/query with transactionId and query
2. Validation in Controller
3. Database lookup for transaction metadata
4. Elasticsearch search with pagination
5. OpenAI analysis
6. Return JSON response

**Error Paths**:
- Transaction not found → Error response
- No logs found → Error response
- OpenAI API failure → Error response

**Startup Sequence**:
1. Application starts
2. Liquibase creates schema and loads data
3. LogIndexingService reads log file
4. Logs parsed and indexed to Elasticsearch in batches

## Customization

You can customize the diagrams by:
- Changing colors (right-click on shape → Format)
- Adding/removing components
- Modifying text and descriptions
- Adding notes and annotations
- Creating new diagrams for specific aspects

## Tips

1. **Export for Documentation**: Export diagrams as PNG for README files
2. **Keep Updated**: Update diagrams when architecture changes
3. **Version Control**: Commit diagram changes with code changes
4. **Multiple Views**: Create separate diagrams for different aspects (deployment, security, etc.)

## Additional Diagrams You Could Create

- **Deployment Diagram**: Docker containers, networking, ports
- **Database Schema**: Entity relationships
- **Sequence Diagram**: Detailed API call sequence
- **Component Diagram**: Package/module structure
- **Network Diagram**: Infrastructure and connections

