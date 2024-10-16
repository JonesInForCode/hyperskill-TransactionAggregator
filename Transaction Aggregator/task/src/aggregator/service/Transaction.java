package aggregator.service;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

public class Transaction implements Comparable<Transaction> {
    private String id;
    private String serverId;
    private String account;
    private String amount;
    private String timestamp;

    // Default constructor
    public Transaction() {}

    // JSON Creator constructor
    @JsonCreator
    public Transaction(
            @JsonProperty("id") String id,
            @JsonProperty("serverId") String serverId,
            @JsonProperty("account") String account,
            @JsonProperty("amount") String amount,
            @JsonProperty("timestamp") String timestamp) {
        this.id = id;
        this.serverId = serverId;
        this.account = account;
        this.amount = amount;
        this.timestamp = timestamp;
    }

    // Getters and setters

    @JsonProperty("id")
    public String getId() {
        return id;
    }

    @JsonProperty("serverId")
    public String getServerId() {
        return serverId;
    }

    @JsonProperty("account")
    public String getAccount() {
        return account;
    }

    @JsonProperty("amount")
    public String getAmount() {
        return amount;
    }

    @JsonProperty("timestamp")
    public String getTimestamp() {
        return timestamp;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setServerId(String serverId) {
        this.serverId = serverId;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public void setAmount(String amount) {
        this.amount = amount;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public int compareTo(Transaction otherTransaction) {
        return this.timestamp.compareTo(otherTransaction.timestamp);
    }
}