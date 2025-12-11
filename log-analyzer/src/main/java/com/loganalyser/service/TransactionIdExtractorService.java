package com.loganalyser.service;

import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class TransactionIdExtractorService {

    // Common transaction ID patterns - prioritized to match log format (TX#########)
    private static final Pattern[] TRANSACTION_PATTERNS = {
        Pattern.compile("\\b(TX\\d{9})\\b"), // TX followed by 9 digits (e.g., TX651750504)
        Pattern.compile("\\b(TX\\d+)\\b"), // TX followed by any digits
        Pattern.compile("transaction[_-]?id[\\s:=]+(TX\\d+)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("txn[_-]?id[\\s:=]+(TX\\d+)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("tx[_-]?id[\\s:=]+(TX\\d+)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("transaction[_-]?id[\\s:=]+([a-zA-Z0-9-]+)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("txn[_-]?id[\\s:=]+([a-zA-Z0-9-]+)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("tx[_-]?id[\\s:=]+([a-zA-Z0-9-]+)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("id[\\s:=]+([a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12})", Pattern.CASE_INSENSITIVE), // UUID
        Pattern.compile("([a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12})", Pattern.CASE_INSENSITIVE), // Standalone UUID
        Pattern.compile("\\b([A-Z0-9]{10,})\\b") // Alphanumeric ID (10+ chars)
    };

    /**
     * Extracts transaction ID from the query string
     * @param query The query string
     * @return The extracted transaction ID, or null if not found
     */
    public String extractTransactionId(String query) {
        if (query == null || query.trim().isEmpty()) {
            return null;
        }

        // Try each pattern
        for (Pattern pattern : TRANSACTION_PATTERNS) {
            Matcher matcher = pattern.matcher(query);
            if (matcher.find()) {
                String transactionId = matcher.group(1);
                if (transactionId != null && !transactionId.trim().isEmpty()) {
                    return transactionId.trim();
                }
            }
        }

        return null;
    }
}

