package com.example.fraud_detector.model;

public class AnalysisResult {
    private Transaction txn;
    private String explanation;
    private String verdict;

    public AnalysisResult() {}

    public AnalysisResult(Transaction txn, String explanation, String verdict) {
        this.txn = txn;
        this.explanation = explanation;
        this.verdict = verdict;
    }

    public Transaction getTxn() {
        return txn;
    }

    public void setTxn(Transaction txn) {
        this.txn = txn;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    public String getVerdict() {
        return verdict;
    }

    public void setVerdict(String verdict) {
        this.verdict = verdict;
    }
}
