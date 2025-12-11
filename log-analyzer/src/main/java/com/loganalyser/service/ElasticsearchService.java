package com.loganalyser.service;

import com.loganalyser.document.LogDocument;
import com.loganalyser.repository.LogDocumentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ElasticsearchService {

    private static final Logger logger = LoggerFactory.getLogger(ElasticsearchService.class);

    private final LogDocumentRepository logDocumentRepository;

    public ElasticsearchService(LogDocumentRepository logDocumentRepository) {
        this.logDocumentRepository = logDocumentRepository;
    }

    /**
     * Search logs by transaction ID with pagination
     * @param transactionId The transaction ID to search for
     * @param page Page number (0-based)
     * @param size Page size
     * @return Page of log documents
     */
    public Page<LogDocument> searchLogsByTransactionId(String transactionId, int page, int size) {
        logger.info("Searching logs for transaction ID: {}, page: {}, size: {}", transactionId, page, size);
        
        Pageable pageable = PageRequest.of(page, size);
        Page<LogDocument> result = logDocumentRepository.findByTransactionId(transactionId, pageable);
        
        logger.info("Found {} logs for transaction ID: {} (total: {})", 
                result.getNumberOfElements(), transactionId, result.getTotalElements());
        
        return result;
    }

    /**
     * Search logs by transaction ID or UUID with pagination
     * @param transactionId The transaction ID
     * @param uuid The UUID
     * @param page Page number (0-based)
     * @param size Page size
     * @return Page of log documents
     */
    public Page<LogDocument> searchLogsByTransactionIdOrUuid(String transactionId, String uuid, int page, int size) {
        logger.info("Searching logs for transaction ID: {} or UUID: {}, page: {}, size: {}", 
                transactionId, uuid, page, size);
        
        Pageable pageable = PageRequest.of(page, size);
        Page<LogDocument> result = logDocumentRepository.findByTransactionIdOrUuid(transactionId, uuid, pageable);
        
        logger.info("Found {} logs (total: {})", result.getNumberOfElements(), result.getTotalElements());
        
        return result;
    }

    /**
     * Get all logs for a transaction ID (fetching in pages)
     * @param transactionId The transaction ID
     * @param pageSize Size of each page
     * @return List of all log lines
     */
    public List<String> getAllLogsByTransactionId(String transactionId, int pageSize) {
        logger.info("Fetching all logs for transaction ID: {} with page size: {}", transactionId, pageSize);
        
        List<String> allLogs = new ArrayList<>();
        int page = 0;
        Page<LogDocument> result;
        
        do {
            Pageable pageable = PageRequest.of(page, pageSize);
            result = logDocumentRepository.findByTransactionId(transactionId, pageable);
            
            for (LogDocument doc : result.getContent()) {
                String logLine = String.format("[Line %d] %s", 
                        doc.getLineNumber() != null ? doc.getLineNumber() : 0, 
                        doc.getLogLine());
                allLogs.add(logLine);
            }
            
            page++;
        } while (result.hasNext());
        
        logger.info("Retrieved {} total log lines for transaction ID: {}", allLogs.size(), transactionId);
        
        return allLogs;
    }

    /**
     * Index a single log document
     * @param logDocument The log document to index
     * @return The indexed document
     */
    public LogDocument indexLog(LogDocument logDocument) {
        logger.debug("Indexing log document for transaction ID: {}", logDocument.getTransactionId());
        return logDocumentRepository.save(logDocument);
    }

    /**
     * Index multiple log documents
     * @param logDocuments List of log documents to index
     * @return Iterable of indexed documents
     */
    public Iterable<LogDocument> indexLogs(List<LogDocument> logDocuments) {
        logger.info("Indexing {} log documents", logDocuments.size());
        Iterable<LogDocument> result = logDocumentRepository.saveAll(logDocuments);
        logger.info("Successfully indexed log documents");
        return result;
    }

    /**
     * Count logs for a transaction ID
     * @param transactionId The transaction ID
     * @return Total count of logs
     */
    public long countLogsByTransactionId(String transactionId) {
        List<LogDocument> logs = logDocumentRepository.findByTransactionId(transactionId);
        return logs.size();
    }
}

