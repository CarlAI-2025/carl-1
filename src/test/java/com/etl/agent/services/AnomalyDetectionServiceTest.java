package com.etl.agent.services;

import com.etl.agent.domain.AnomalyReport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Anomaly Detection Service (Gold tier).
 */
public class AnomalyDetectionServiceTest {
    private AnomalyDetectionService service;

    @BeforeEach
    public void setUp() {
        service = new AnomalyDetectionService();
    }

    @Test
    public void testOutlierDetectionWithZScore() {
        List<Double> values = Arrays.asList(
                100.0, 102.0, 101.0, 103.0, 99.0,
                100.0, 101.0, 102.0, 100.0, 500.0 // Outlier
        );

        List<AnomalyReport> reports = service.detectOutliers("amount", values);

        assertTrue(reports.size() > 0);
        assertEquals("OUTLIER_Z_SCORE", reports.get(0).getAnomalyType());
        assertTrue(reports.get(0).getConfidenceScore() > 0.5);
    }

    @Test
    public void testDistributionSkewnessDetection() {
        List<Double> values = Arrays.asList(
                1.0, 2.0, 2.0, 3.0, 3.0, 3.0, 4.0, 4.0, 4.0, 4.0,
                5.0, 5.0, 5.0, 100.0, 200.0, 300.0 // High skew
        );

        List<AnomalyReport> reports = service.detectDistributionAnomalies("amount", values);

        assertTrue(reports.size() > 0);
        assertEquals("DISTRIBUTION_SKEW", reports.get(0).getAnomalyType());
    }

    @Test
    public void testExcessiveNullsDetection() {
        List<Object> values = Arrays.asList(
                "value1", null, null, null, null,
                null, "value2", null, null, null
        );

        List<AnomalyReport> reports = service.detectMissingValueAnomalies("field", values);

        assertTrue(reports.size() > 0);
        assertEquals("EXCESSIVE_NULLS", reports.get(0).getAnomalyType());
        assertTrue(reports.get(0).getStatisticalMetrics().get("null_percentage") > 50);
    }

    @Test
    public void testLowCardinalityDetection() {
        List<String> values = Arrays.asList(
                "A", "A", "A", "A", "A",
                "A", "A", "A", "A", "B"
        );

        List<AnomalyReport> reports = service.detectCardinalityAnomalies("category", values);

        assertTrue(reports.size() > 0);
        assertEquals("LOW_CARDINALITY", reports.get(0).getAnomalyType());
    }

    @Test
    public void testHighCardinalityDetection() {
        List<String> values = new java.util.ArrayList<>();
        for (int i = 0; i < 100; i++) {
            values.add("unique_value_" + i);
        }

        List<AnomalyReport> reports = service.detectCardinalityAnomalies("id", values);

        assertTrue(reports.size() > 0);
        assertEquals("HIGH_CARDINALITY", reports.get(0).getAnomalyType());
    }

    @Test
    public void testNoAnomaliesWithCleanData() {
        List<Double> values = Arrays.asList(
                100.0, 101.0, 99.0, 100.0, 102.0,
                99.0, 101.0, 100.0, 100.0, 101.0
        );

        List<AnomalyReport> reports = service.detectOutliers("amount", values);

        assertEquals(0, reports.size());
    }

    @Test
    public void testMultipleAnomalyTypes() {
        // Test with multiple anomalies
        List<Double> skewedValues = Arrays.asList(
                1.0, 1.0, 1.0, 2.0, 2.0, 3.0, 100.0, 200.0
        );

        List<AnomalyReport> outliers = service.detectOutliers("amount", skewedValues);
        List<AnomalyReport> distribution = service.detectDistributionAnomalies("amount", skewedValues);

        assertTrue(outliers.size() > 0);
        assertTrue(distribution.size() > 0);
    }

    @Test
    public void testSeverityClassification() {
        List<Double> values = Arrays.asList(
                100.0, 101.0, 99.0, 100.0, 102.0, 99.0, 101.0, 100.0, 1000.0
        );

        List<AnomalyReport> reports = service.detectOutliers("amount", values);

        assertFalse(reports.isEmpty());
        AnomalyReport.AnomalySeverity severity = reports.get(0).getSeverity();
        assertNotNull(severity);
        assertTrue(severity == AnomalyReport.AnomalySeverity.MEDIUM ||
                   severity == AnomalyReport.AnomalySeverity.HIGH);
    }

    @Test
    public void testSuggestedTransformations() {
        List<Double> values = Arrays.asList(
                100.0, 101.0, 99.0, 100.0, 102.0, 99.0, 101.0, 100.0, 10000.0
        );

        List<AnomalyReport> reports = service.detectOutliers("amount", values);

        assertFalse(reports.isEmpty());
        assertTrue(reports.get(0).getSuggestedTransformations().size() > 0);
    }
}

