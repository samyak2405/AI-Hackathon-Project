package com.loganalyser.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Document(indexName = "application-logs")
public class LogDocument {

    @Id
    private String id;

    @Field(type = FieldType.Text, name = "log_line")
    private String logLine;

    @Field(type = FieldType.Text, name = "transaction_id")
    private String transactionId;

    @Field(type = FieldType.Text, name = "uuid")
    private String uuid;

    @Field(type = FieldType.Text, name = "client_txn_id")
    private String clientTxnId;

    @Field(type = FieldType.Text, name = "user_id")
    private String userId;

    @Field(type = FieldType.Text, name = "log_level")
    private String logLevel;

    @Field(type = FieldType.Text, name = "service")
    private String service;

    @Field(type = FieldType.Date, name = "timestamp")
    private LocalDate timestamp;

    @Field(type = FieldType.Integer, name = "line_number")
    private Integer lineNumber;

    // Constructors
    public LogDocument() {
    }

    public LogDocument(String logLine, String transactionId, Integer lineNumber) {
        this.logLine = logLine;
        this.transactionId = transactionId;
        this.lineNumber = lineNumber;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getLogLine() {
        return logLine;
    }

    public void setLogLine(String logLine) {
        this.logLine = logLine;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getClientTxnId() {
        return clientTxnId;
    }

    public void setClientTxnId(String clientTxnId) {
        this.clientTxnId = clientTxnId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getLogLevel() {
        return logLevel;
    }

    public void setLogLevel(String logLevel) {
        this.logLevel = logLevel;
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public LocalDate getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDate timestamp) {
        this.timestamp = timestamp;
    }

    public Integer getLineNumber() {
        return lineNumber;
    }

    public void setLineNumber(Integer lineNumber) {
        this.lineNumber = lineNumber;
    }
}

