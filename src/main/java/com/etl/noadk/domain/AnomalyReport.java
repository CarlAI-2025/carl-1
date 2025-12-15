package com.etl.noadk.domain;

import com.google.gson.annotations.SerializedName;
import java.util.*;

/**
 * Anomaly detection report for Gold tier functionality.
 */
public class AnomalyReport {
    @SerializedName("report_id")
    private String reportId;

    @SerializedName("field_name")
    private String fieldName;

    @SerializedName("anomaly_type")
    private String anomalyType;

    @SerializedName("severity")
    private AnomalySeverity severity;

    @SerializedName("description")
    private String description;

    @SerializedName("affected_records")
    private List<String> affectedRecords;

    @SerializedName("suggested_transformations")
    private List<String> suggestedTransformations;

    @SerializedName("statistical_metrics")
    private Map<String, Double> statisticalMetrics;

    @SerializedName("confidence_score")
    private double confidenceScore;

    public enum AnomalySeverity {
        LOW, MEDIUM, HIGH, CRITICAL
    }

    public AnomalyReport() {
        this.reportId = UUID.randomUUID().toString();
        this.affectedRecords = new ArrayList<>();
        this.suggestedTransformations = new ArrayList<>();
        this.statisticalMetrics = new HashMap<>();
    }

    // Getters and setters
    public String getReportId() { return reportId; }

    public String getFieldName() { return fieldName; }
    public void setFieldName(String fieldName) { this.fieldName = fieldName; }

    public String getAnomalyType() { return anomalyType; }
    public void setAnomalyType(String anomalyType) { this.anomalyType = anomalyType; }

    public AnomalySeverity getSeverity() { return severity; }
    public void setSeverity(AnomalySeverity severity) { this.severity = severity; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public List<String> getAffectedRecords() { return affectedRecords; }

    public List<String> getSuggestedTransformations() { return suggestedTransformations; }

    public Map<String, Double> getStatisticalMetrics() { return statisticalMetrics; }

    public double getConfidenceScore() { return confidenceScore; }
    public void setConfidenceScore(double confidenceScore) { this.confidenceScore = confidenceScore; }
}

