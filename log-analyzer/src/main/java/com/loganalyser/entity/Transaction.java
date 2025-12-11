package com.loganalyser.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "transactions")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_id", unique = true, nullable = false)
    private String transactionId;

    @Column(name = "uuid")
    private String uuid;

    @Column(name = "service_id")
    private String serviceId;

    // Constructors
    public Transaction() {
    }

    public Transaction(String transactionId, String uuid, String serviceId) {
        this.transactionId = transactionId;
        this.uuid = uuid;
        this.serviceId = serviceId;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getServiceId() {
        return serviceId;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    @Override
    public String toString() {
        return "Transaction{" +
                "id=" + id +
                ", transactionId='" + transactionId + '\'' +
                ", uuid='" + uuid + '\'' +
                ", serviceId='" + serviceId + '\'' +
                '}';
    }
}

