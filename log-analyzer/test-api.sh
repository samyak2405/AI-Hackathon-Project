#!/bin/bash

# Test script for Log Analyser API
# Make sure the application is running on port 8080

BASE_URL="http://localhost:8080/api"

echo "=========================================="
echo "Testing Log Analyser API"
echo "=========================================="
echo ""

# Test 1: Health check
echo "1. Testing Health Check Endpoint:"
echo "-----------------------------------"
curl -X GET "${BASE_URL}/health" \
  -H "Content-Type: application/json" \
  -w "\nHTTP Status: %{http_code}\n" \
  -s
echo ""
echo ""

# Test 2: Query with transaction ID from database
echo "2. Testing Query Endpoint (Transaction from DB):"
echo "-----------------------------------"
echo "Request: Transaction ID = TX872000310, Query = 'What happened with this transaction?'"
echo ""
curl -X POST "${BASE_URL}/query" \
  -H "Content-Type: application/json" \
  -d '{
    "transactionId": "TX872000310",
    "query": "What happened with this transaction? Analyze the logs and provide details."
  }' \
  -w "\nHTTP Status: %{http_code}\n" \
  -s | jq '.' 2>/dev/null || cat
echo ""
echo ""

# Test 3: Query with another transaction ID
echo "3. Testing Query Endpoint (Another Transaction):"
echo "-----------------------------------"
echo "Request: Transaction ID = TX651750504, Query = 'What was the status of this transaction?'"
echo ""
curl -X POST "${BASE_URL}/query" \
  -H "Content-Type: application/json" \
  -d '{
    "transactionId": "TX651750504",
    "query": "What was the status of this transaction? Was it successful or failed?"
  }' \
  -w "\nHTTP Status: %{http_code}\n" \
  -s | jq '.' 2>/dev/null || cat
echo ""
echo ""

# Test 4: Query with non-existent transaction ID
echo "4. Testing Query Endpoint (Non-existent Transaction):"
echo "-----------------------------------"
echo "Request: Transaction ID = TX999999999, Query = 'What happened?'"
echo ""
curl -X POST "${BASE_URL}/query" \
  -H "Content-Type: application/json" \
  -d '{
    "transactionId": "TX999999999",
    "query": "What happened with this transaction?"
  }' \
  -w "\nHTTP Status: %{http_code}\n" \
  -s | jq '.' 2>/dev/null || cat
echo ""
echo ""

# Test 5: Invalid request (missing transactionId)
echo "5. Testing Query Endpoint (Missing transactionId):"
echo "-----------------------------------"
echo "Request: Missing transactionId field"
echo ""
curl -X POST "${BASE_URL}/query" \
  -H "Content-Type: application/json" \
  -d '{
    "query": "What happened?"
  }' \
  -w "\nHTTP Status: %{http_code}\n" \
  -s | jq '.' 2>/dev/null || cat
echo ""
echo ""

echo "=========================================="
echo "Testing Complete"
echo "=========================================="

