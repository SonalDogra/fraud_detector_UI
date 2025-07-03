package com.example.fraud_detector.model;

import java.util.List;

public class Transaction {
    private String consumerId;
    private double amount;
    private String location;
    private int hour;
    private String deviceId;
    private List<String> knownLocations;
    private List<String> knownDevices;
    private double averageTransactionAmount;
    private List<String> recentFlags;

    public Transaction() {}

    public Transaction(String consumerId, double amount, String location, int hour,
                       String deviceId, List<String> knownLocations, List<String> knownDevices,
                       double averageTransactionAmount, List<String> recentFlags) {
        this.consumerId = consumerId;
        this.amount = amount;
        this.location = location;
        this.hour = hour;
        this.deviceId = deviceId;
        this.knownLocations = knownLocations;
        this.knownDevices = knownDevices;
        this.averageTransactionAmount = averageTransactionAmount;
        this.recentFlags = recentFlags;
    }

    // Getters and Setters
    public String getConsumerId() { return consumerId; }
    public void setConsumerId(String consumerId) { this.consumerId = consumerId; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public int getHour() { return hour; }
    public void setHour(int hour) { this.hour = hour; }

    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }

    public List<String> getKnownLocations() { return knownLocations; }
    public void setKnownLocations(List<String> knownLocations) { this.knownLocations = knownLocations; }

    public List<String> getKnownDevices() { return knownDevices; }
    public void setKnownDevices(List<String> knownDevices) { this.knownDevices = knownDevices; }

    public double getAverageTransactionAmount() { return averageTransactionAmount; }
    public void setAverageTransactionAmount(double averageTransactionAmount) { this.averageTransactionAmount = averageTransactionAmount; }

    public List<String> getRecentFlags() { return recentFlags; }
    public void setRecentFlags(List<String> recentFlags) { this.recentFlags = recentFlags; }
}
