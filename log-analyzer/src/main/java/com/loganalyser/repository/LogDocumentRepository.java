package com.loganalyser.repository;

import com.loganalyser.document.LogDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LogDocumentRepository extends ElasticsearchRepository<LogDocument, String> {
    
    Page<LogDocument> findByTransactionId(String transactionId, Pageable pageable);
    
    List<LogDocument> findByTransactionId(String transactionId);
    
    Page<LogDocument> findByTransactionIdOrUuid(String transactionId, String uuid, Pageable pageable);
}

