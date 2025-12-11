## User Service (Spring Boot 3, Java 21, JWT, Postgres)

### Run locally (no Docker)

1. Ensure Postgres is running locally with:
   - **DB**: `user_service_db`
   - **User**: `user_service`
   - **Password**: `user_service_password`
2. From `user-service` folder:
   ```bash
   mvn spring-boot:run
   ```

### Run with Docker / Docker Compose

From the `user-service` folder:

```bash
docker compose up --build
```

This will start:
- **Postgres** on `localhost:5432`
- **User Service** on `localhost:8081`

### Key Endpoints

- **Register**: `POST /api/auth/register`
  - Body:
    ```json
    {
      "username": "john",
      "email": "john@example.com",
      "password": "Password123!",
      "role": "CUSTOMER"
    }
    ```
- **Login**: `POST /api/auth/login`
  - Body:
    ```json
    {
      "username": "john",
      "password": "Password123!"
    }
    ```
- **Current user (for welcome page)**: `GET /api/auth/me`
  - Header: `Authorization: Bearer <jwt-token-from-login-or-register>`


## Prompt for design
You are a pro in designing architecture on draw.io Goal: Based on the system description below give a robust solution and draw.io code System: We are building a ChatBot application. where user enters the prompt and he gets the response. This application in backend talks to AI Agent (router agent) which processes the user prompt and decides whether to go to RCA Service or Data Service RCA Service: Service which based on the query that user has given, processes logs from multiple microservices and gives the root cause of the issue that might have occurred for example: Let's say there is a transaction that might have failed due to db connectivity error User enters a prompt that, Why transaction against mobile number 8551234212 failed give me RCA for that RCA Service fetches the right microservices logs, processes it and gives the RCA as response. Inorder to process the RCA there is an AI Agent sitting in that service which does this processing DataService: For Example: If user says I want data of last 10 transactions done -> Router agent redirects thsi query to DataService Where DataAgent will process the prompt and give the output by creating a query, executing it in db and sends response back to user Now using this data i want you to design a robust System Design which involves concepts like RAG, Multi-AI-Agent modeling, Model Context Protocol Client and server architecture, etc Improve the system as much as you can and give me good draw.io code for architecture

## Prompt for Presentation
You are a senior backend architect and expert technical presenter.
Create a detailed PowerPoint presentation for the following system.

Context

We are building an AI-powered ChatBot application.

A user enters a natural-language prompt in the chatbot UI and receives a response.

The backend sends each user prompt to an AI Router Agent.

The Router Agent decides whether the request should go to:

an RCA Service, or

a Data Service.

Core Services

RCA Service (Root Cause Analysis Service)

Purpose: Analyze application logs from multiple microservices and provide root cause analysis (RCA) for failures, errors, and anomalies.

Example:

A transaction fails due to a DB connectivity error.

User prompt: “Why did the transaction against mobile number 8551234212 fail? Give me the RCA.”

The RCA Service:

Fetches the relevant logs from the correct microservices.

Processes logs using an internal AI RCA Agent.

Produces a detailed RCA, including probable cause and possible fixes.

The RCA Service should leverage:

RAG (Retrieval-Augmented Generation) on log data and documentation.

Vector database for log chunks / embeddings.

Metadata-based search (e.g., by transaction ID, time window, microservice name).

Data Service

Purpose: Answer data/analytics queries based on the transactional database.

Example:

User prompt: “Give me the last 10 transactions done for user X.”

The Router Agent sends this to the Data Service.

Inside the Data Service, a Data Agent:

Interprets the natural language prompt.

Generates the appropriate database query.

Executes the query on the DB.

Returns the formatted result to the user.

The Data Service should use:

Model Context Protocol (MCP) Client and Server to connect the AI Agent with the database.

MCP Server exposes tools/endpoints for safe DB access (e.g., “runParameterizedQuery”, “getRecentTransactions”).

The Data Agent calls these MCP tools to fetch data.

Advanced Requirements & Improvements

Evolve and enhance the system with the following concepts:

Multi-Agent AI Modeling

Router Agent to classify and route user intents.

RCA Agent specialized in log analysis and RCA generation.

Data Agent specialized in analytical/data queries.

Optional: Knowledge Agent to handle FAQs and documentation queries.

Retrieval-Augmented Generation (RAG) for Logs & Knowledge

Ingest logs from multiple microservices into a log store (e.g., object storage + search/analytics engine + vector DB).

Chunk and embed logs with metadata (service name, timestamps, trace IDs, transaction IDs).

Use RAG in the RCA Service to:

Retrieve relevant log chunks and documentation.

Provide grounded, evidence-based RCA responses.

Model Context Protocol (MCP) Architecture

MCP Server:

Exposes tools to query databases, log stores, ticketing systems, and monitoring tools.

MCP Client (inside agents):

Calls tools like:

fetchLogsByTraceId

fetchLastNTransactions

getErrorRateForService

Use MCP to keep agents stateless but powerful, with secure and audited access to backend systems.

Non-Functional Aspects

High-level mention of:

Scalability (horizontal scaling of agents, services, and vector stores).

Observability (centralized logging, metrics, tracing).

Security (authentication/authorization for agents, data masking/PII protection).

Fault tolerance and retries.

Rate limiting and quota management for AI calls.

Presentation Requirements

Create a clear, structured PPT with the following:

Slide 1: Title Slide

Title: “AI-Driven ChatBot for RCA and Data Insights”

Subtitle: “Multi-Agent, RAG and MCP-Enabled Backend Architecture”

Include space for my name, role, and date.

Slide 2: Problem Statement

What problems this system solves:

Developers/support teams need quick RCA from logs.

Business/ops users need fast access to transactional data and analytics.

Pain points of traditional/manual approaches.

Slide 3: High-Level Overview

End-to-end flow:

User → ChatBot UI → Router Agent → RCA Service / Data Service → Response to user.

Simple, conceptual diagram.

Slide 4: Router Agent & Intent Classification

Explain how the Router Agent classifies requests (RCA vs Data vs others).

Mention high-level rules and examples.

Show a simple flow diagram or decision tree.

Slide 5–6: RCA Service with RAG

Deep-dive on the RCA Service:

Log ingestion, indexing, and embedding.

Vector DB + metadata store.

RCA Agent using RAG to generate explanations.

Example flow for the failed transaction with mobile number 8551234212.

Slide 7–8: Data Service with MCP

Deep-dive on the Data Service:

Data Agent understanding the query.

Use of MCP Client → MCP Server → database tools.

Safe query execution and result formatting.

Example flow: “Last 10 transactions” query.

Slide 9: Multi-Agent Architecture

Show how Router, RCA, Data, and optional Knowledge Agents interact.

Discuss benefits: separation of concerns, specialization, easier scaling.

Slide 10: System Components & Technology Stack

List possible technologies:

Backend: Java, Spring Boot, REST/gRPC.

AI: LLM provider of your choice.

Vector DB: e.g., Pinecone / Milvus / Weaviate.

Log store/search: e.g., OpenSearch / Elasticsearch / ClickHouse.

MCP framework for tools.

Message broker (optional) for async processing (Kafka, etc.).

Slide 11: Non-Functional Requirements

Scalability, reliability, observability, security, compliance (PII and access control).

How to handle high traffic and large log volumes.

Slide 12: End-to-End Example User Journeys

1 RCA example (failed transaction).

1 Data example (analytics query).

Show how the system behaves from user prompt to final answer.

Slide 13: Future Enhancements

Ideas like:

Auto-ticket creation in Jira for genuine system issues.

Feedback loop to improve agents.

Integration with monitoring/alerting tools (Prometheus, Grafana, etc.).

Slide 14: Summary

Recap key ideas: AI Router, RCA Service with RAG, Data Service with MCP, multi-agent pattern, and benefits.

Formatting & Style

Use a clean, modern, professional template.

Keep text concise and use bullet points.

Include at least 2–3 architecture/flow diagrams to visualize the system.

Add speaker notes for each slide explaining what should be spoken in a presentation.

Target audience: senior engineers, architects, and technical managers.

Generate the full slide-by-slide content (titles, bullet points, and speaker notes).

You can tweak slide count or wording, but keep the structure and technical depth.