package com.etl.agent.services;

import com.etl.agent.domain.AnomalyReport;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Anomaly detection service for Gold tier functionality.
 * Performs statistical analysis on numeric indicators.
 */
@Slf4j
public class AnomalyDetectionService {
    private static final double Z_SCORE_THRESHOLD = 3.0;
    private static final double IQR_MULTIPLIER = 1.5;

    /**
     * Detect outliers using Z-score method.
     */
    public List<AnomalyReport> detectOutliers(String fieldName, List<Double> values) {
        List<AnomalyReport> reports = new ArrayList<>();

        if (values.size() < 2) {
            return reports;
        }

        double mean = calculateMean(values);
        double stdDev = calculateStdDev(values, mean);

        if (stdDev == 0) {
            return reports; // All values identical
        }

        for (int i = 0; i < values.size(); i++) {
            double value = values.get(i);
            double zScore = Math.abs((value - mean) / stdDev);

            if (zScore > Z_SCORE_THRESHOLD) {
                AnomalyReport report = new AnomalyReport();
                report.setFieldName(fieldName);
                report.setAnomalyType("OUTLIER_Z_SCORE");
                report.setSeverity(zScore > 4.0 ? AnomalyReport.AnomalySeverity.CRITICAL : AnomalyReport.AnomalySeverity.MEDIUM);
                report.setDescription(String.format("Value %.2f is %.2f standard deviations from mean", value, zScore));
                report.getAffectedRecords().add(String.valueOf(i));
                report.getStatisticalMetrics().put("z_score", zScore);
                report.getStatisticalMetrics().put("mean", mean);
                report.getStatisticalMetrics().put("stddev", stdDev);
                report.setConfidenceScore(Math.min(1.0, zScore / 5.0));

                // Suggest transformations
                if (value < mean) {
                    report.getSuggestedTransformations().add("REPLACE_WITH_MEAN");
                    report.getSuggestedTransformations().add("REPLACE_WITH_MEDIAN");
                    report.getSuggestedTransformations().add("LOG_TRANSFORM");
                } else {
                    report.getSuggestedTransformations().add("CAP_AT_THRESHOLD");
                    report.getSuggestedTransformations().add("QUANTILE_NORMALIZE");
                }

                reports.add(report);
            }
        }

        log.info("Detected {} outliers in field {}", reports.size(), fieldName);
        return reports;
    }

    /**
     * Detect distribution anomalies (skewness, kurtosis).
     */
    public List<AnomalyReport> detectDistributionAnomalies(String fieldName, List<Double> values) {
        List<AnomalyReport> reports = new ArrayList<>();

        if (values.size() < 10) {
            return reports;
        }

        double skewness = calculateSkewness(values);
        double kurtosis = calculateKurtosis(values);

        // High skewness indicates asymmetric distribution
        if (Math.abs(skewness) > 2.0) {
            AnomalyReport report = new AnomalyReport();
            report.setFieldName(fieldName);
            report.setAnomalyType("DISTRIBUTION_SKEW");
            report.setSeverity(Math.abs(skewness) > 3.0 ? AnomalyReport.AnomalySeverity.HIGH : AnomalyReport.AnomalySeverity.MEDIUM);
            report.setDescription(String.format("Distribution skewness: %.2f", skewness));
            report.getStatisticalMetrics().put("skewness", skewness);
            report.getStatisticalMetrics().put("kurtosis", kurtosis);
            report.setConfidenceScore(0.85);

            report.getSuggestedTransformations().add("LOG_TRANSFORM");
            report.getSuggestedTransformations().add("BOX_COX_TRANSFORM");
            report.getSuggestedTransformations().add("QUANTILE_NORMALIZATION");

            reports.add(report);
        }

        return reports;
    }

    /**
     * Detect missing value patterns.
     */
    public List<AnomalyReport> detectMissingValueAnomalies(String fieldName, List<Object> values) {
        List<AnomalyReport> reports = new ArrayList<>();

        long nullCount = values.stream().filter(v -> v == null).count();
        double nullPercentage = (double) nullCount / values.size() * 100;

        if (nullPercentage > 10.0) { // More than 10% null
            AnomalyReport report = new AnomalyReport();
            report.setFieldName(fieldName);
            report.setAnomalyType("EXCESSIVE_NULLS");
            report.setSeverity(nullPercentage > 50.0 ? AnomalyReport.AnomalySeverity.CRITICAL : AnomalyReport.AnomalySeverity.HIGH);
            report.setDescription(String.format("Field has %.2f%% null values", nullPercentage));
            report.getStatisticalMetrics().put("null_percentage", nullPercentage);
            report.setConfidenceScore(0.95);

            report.getSuggestedTransformations().add("REMOVE_FIELD");
            report.getSuggestedTransformations().add("IMPUTE_MEAN");
            report.getSuggestedTransformations().add("IMPUTE_MEDIAN");
            report.getSuggestedTransformations().add("IMPUTE_FORWARD_FILL");

            reports.add(report);
        }

        return reports;
    }

    /**
     * Detect cardinality anomalies (too many/few unique values).
     */
    public List<AnomalyReport> detectCardinalityAnomalies(String fieldName, List<String> values) {
        List<AnomalyReport> reports = new ArrayList<>();

        Set<String> uniqueValues = new HashSet<>(values);
        double cardinalityRatio = (double) uniqueValues.size() / values.size();

        // Too low cardinality (near-constant)
        if (cardinalityRatio < 0.01 && uniqueValues.size() < 5) {
            AnomalyReport report = new AnomalyReport();
            report.setFieldName(fieldName);
            report.setAnomalyType("LOW_CARDINALITY");
            report.setSeverity(AnomalyReport.AnomalySeverity.MEDIUM);
            report.setDescription(String.format("Only %d unique values in %d records", uniqueValues.size(), values.size()));
            report.getStatisticalMetrics().put("unique_count", (double) uniqueValues.size());
            report.getStatisticalMetrics().put("cardinality_ratio", cardinalityRatio);
            report.setConfidenceScore(0.9);

            report.getSuggestedTransformations().add("DROP_FIELD");
            report.getSuggestedTransformations().add("CONSOLIDATE_CATEGORIES");

            reports.add(report);
        }

        // Too high cardinality (nearly unique)
        if (cardinalityRatio > 0.95) {
            AnomalyReport report = new AnomalyReport();
            report.setFieldName(fieldName);
            report.setAnomalyType("HIGH_CARDINALITY");
            report.setSeverity(AnomalyReport.AnomalySeverity.LOW);
            report.setDescription(String.format("Nearly all values are unique: %.2f cardinality", cardinalityRatio));
            report.getStatisticalMetrics().put("unique_count", (double) uniqueValues.size());
            report.getStatisticalMetrics().put("cardinality_ratio", cardinalityRatio);
            report.setConfidenceScore(0.85);

            report.getSuggestedTransformations().add("HASH_ENCODING");
            report.getSuggestedTransformations().add("FREQUENCY_ENCODING");

            reports.add(report);
        }

        return reports;
    }

    // Statistical helpers
    private double calculateMean(List<Double> values) {
        return values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
    }

    private double calculateStdDev(List<Double> values, double mean) {
        double variance = values.stream()
                .mapToDouble(v -> Math.pow(v - mean, 2))
                .average()
                .orElse(0.0);
        return Math.sqrt(variance);
    }

    private double calculateSkewness(List<Double> values) {
        double mean = calculateMean(values);
        double stdDev = calculateStdDev(values, mean);
        double n = values.size();

        if (stdDev == 0) return 0;

        double skew = values.stream()
                .mapToDouble(v -> Math.pow((v - mean) / stdDev, 3))
                .sum() / n;

        return skew;
    }

    private double calculateKurtosis(List<Double> values) {
        double mean = calculateMean(values);
        double stdDev = calculateStdDev(values, mean);
        double n = values.size();

        if (stdDev == 0) return 0;

        double kurt = values.stream()
                .mapToDouble(v -> Math.pow((v - mean) / stdDev, 4))
                .sum() / n - 3;

        return kurt;
    }
}

