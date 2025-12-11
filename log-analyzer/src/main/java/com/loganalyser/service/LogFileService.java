package com.loganalyser.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class LogFileService {

    private static final Logger logger = LoggerFactory.getLogger(LogFileService.class);

    @Value("${log.file.path}")
    private String logFilePath;

    /**
     * Greps all log lines containing the transaction ID
     * @param transactionId The transaction ID to search for
     * @return List of log lines containing the transaction ID
     */
    public List<String> grepLogsByTransactionId(String transactionId) {
        List<String> matchingLogs = new ArrayList<>();

        if (transactionId == null || transactionId.trim().isEmpty()) {
            logger.warn("Transaction ID is null or empty");
            return matchingLogs;
        }

        File logFile = new File(logFilePath);
        if (!logFile.exists()) {
            logger.error("Log file not found at path: {}", logFilePath);
            return matchingLogs;
        }

        if (!logFile.canRead()) {
            logger.error("Cannot read log file at path: {}", logFilePath);
            return matchingLogs;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(logFile))) {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.contains(transactionId)) {
                    matchingLogs.add(String.format("[Line %d] %s", lineNumber, line));
                }
            }
            logger.info("Found {} log lines matching transaction ID: {}", matchingLogs.size(), transactionId);
        } catch (IOException e) {
            logger.error("Error reading log file: {}", e.getMessage(), e);
        }

        return matchingLogs;
    }
}

