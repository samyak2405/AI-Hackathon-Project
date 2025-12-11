package com.loganalyser.service;

import com.loganalyser.document.LogDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class LogIndexingService implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(LogIndexingService.class);

    private final ElasticsearchService elasticsearchService;

    @Value("${log.file.path}")
    private String logFilePath;

    @Value("${elasticsearch.index.on.startup:true}")
    private boolean indexOnStartup;

    // Pattern to extract transaction ID, UUID, etc. from log line
    private static final Pattern TX_ID_PATTERN = Pattern.compile("TX_ID:\\s*(TX\\d+)");
    private static final Pattern UUID_PATTERN = Pattern.compile("UUID:\\s*([a-f0-9-]{36})");
    private static final Pattern CLIENT_TXN_PATTERN = Pattern.compile("CLIENT_TXN_ID:\\s*(CLIENT_TXN_\\d+)");
    private static final Pattern USER_ID_PATTERN = Pattern.compile("USER_ID:\\s*(USER_\\d+)");
    private static final Pattern LOG_LEVEL_PATTERN = Pattern.compile("\\[(INFO|DEBUG|ERROR|WARN|FATAL)\\]");
    private static final Pattern SERVICE_PATTERN = Pattern.compile("\\[(.*?)\\]");
    private static final Pattern TIMESTAMP_PATTERN = Pattern.compile("^(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{3})");
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    public LogIndexingService(ElasticsearchService elasticsearchService) {
        this.elasticsearchService = elasticsearchService;
    }

    @Override
    public void run(String... args) throws Exception {
        if (indexOnStartup) {
            logger.info("Starting log indexing on startup...");
            indexLogsFromFile();
        } else {
            logger.info("Log indexing on startup is disabled. Set elasticsearch.index.on.startup=true to enable.");
        }
    }

    /**
     * Index all logs from the log file to Elasticsearch
     */
    public void indexLogsFromFile() {
        File logFile = new File(logFilePath);
        if (!logFile.exists()) {
            logger.warn("Log file not found at path: {}. Skipping indexing.", logFilePath);
            return;
        }

        if (!logFile.canRead()) {
            logger.error("Cannot read log file at path: {}. Skipping indexing.", logFilePath);
            return;
        }

        logger.info("Starting to index logs from file: {}", logFilePath);

        List<LogDocument> batch = new ArrayList<>();
        int batchSize = 100;
        int lineNumber = 0;
        int totalIndexed = 0;

        try (BufferedReader reader = new BufferedReader(new FileReader(logFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                LogDocument logDoc = parseLogLine(line, lineNumber);
                
                if (logDoc != null) {
                    batch.add(logDoc);
                    
                    // Index in batches
                    if (batch.size() >= batchSize) {
                        elasticsearchService.indexLogs(batch);
                        totalIndexed += batch.size();
                        batch.clear();
                        logger.debug("Indexed batch: {} logs processed", totalIndexed);
                    }
                }
            }

            // Index remaining logs
            if (!batch.isEmpty()) {
                elasticsearchService.indexLogs(batch);
                totalIndexed += batch.size();
            }

            logger.info("Successfully indexed {} log entries from {} total lines", totalIndexed, lineNumber);
        } catch (IOException e) {
            logger.error("Error reading log file for indexing: {}", e.getMessage(), e);
        }
    }

    /**
     * Parse a log line and extract information into LogDocument
     */
    private LogDocument parseLogLine(String logLine, int lineNumber) {
        LogDocument doc = new LogDocument();
        doc.setLogLine(logLine);
        doc.setLineNumber(lineNumber);

        // Extract timestamp
        Matcher timestampMatcher = TIMESTAMP_PATTERN.matcher(logLine);
        if (timestampMatcher.find()) {
            try {
                LocalDate timestamp = LocalDate.parse(timestampMatcher.group(1), TIMESTAMP_FORMATTER);
                doc.setTimestamp(timestamp);
            } catch (DateTimeParseException e) {
                logger.debug("Could not parse timestamp from line: {}", logLine);
            }
        }

        // Extract log level
        Matcher levelMatcher = LOG_LEVEL_PATTERN.matcher(logLine);
        if (levelMatcher.find()) {
            doc.setLogLevel(levelMatcher.group(1));
        }

        // Extract service name (usually in brackets after log level)
        Matcher serviceMatcher = SERVICE_PATTERN.matcher(logLine);
        List<String> services = new ArrayList<>();
        while (serviceMatcher.find() && services.size() < 2) {
            String match = serviceMatcher.group(1);
            if (!match.equals(doc.getLogLevel())) {
                services.add(match);
            }
        }
        if (!services.isEmpty()) {
            doc.setService(services.get(0));
        }

        // Extract transaction ID
        Matcher txIdMatcher = TX_ID_PATTERN.matcher(logLine);
        if (txIdMatcher.find()) {
            doc.setTransactionId(txIdMatcher.group(1));
        }

        // Extract UUID
        Matcher uuidMatcher = UUID_PATTERN.matcher(logLine);
        if (uuidMatcher.find()) {
            doc.setUuid(uuidMatcher.group(1));
        }

        // Extract CLIENT_TXN_ID
        Matcher clientTxnMatcher = CLIENT_TXN_PATTERN.matcher(logLine);
        if (clientTxnMatcher.find()) {
            doc.setClientTxnId(clientTxnMatcher.group(1));
        }

        // Extract USER_ID
        Matcher userIdMatcher = USER_ID_PATTERN.matcher(logLine);
        if (userIdMatcher.find()) {
            doc.setUserId(userIdMatcher.group(1));
        }

        return doc;
    }
}

