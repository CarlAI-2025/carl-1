package com.etl.agent.agents;

import com.etl.agent.domain.*;
import com.google.cloud.bigquery.*;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * Auditor Agent: Data quality, compliance, and reasoning logs.
 * Responsibilities:
 * - DQ scorecard (table)
 * - Compliance score (0–100)
 * - Reasoning log (mapping & transformation rationale)
 */
@Slf4j
public class AuditorAgent implements ETLAgent {
    private final BigQuery bigQuery;

    public AuditorAgent() {
        this.bigQuery = BigQueryOptions.getDefaultInstance().getService();
    }

    @Override
    public String getName() {
        return "Auditor";
    }

    @Override
    public void execute(PipelineJob job) throws Exception {
        log.info("Auditor: Evaluating data quality and compliance");
        long startTime = System.currentTimeMillis();

        try {
            // Calculate DQ Score
            double dqScore = calculateDataQualityScore(job);

            // Calculate Compliance Score
            double complianceScore = calculateComplianceScore(job);

            // Generate reasoning log
            String reasoningLog = generateReasoningLog(job, dqScore, complianceScore);

            // Store audit results
            storeAuditResults(job, dqScore, complianceScore, reasoningLog);

            // Add lineage
            PipelineJob.LineageEntry entry = new PipelineJob.LineageEntry("AUDIT", "Auditor");
            entry.setInputRecords(job.getStatistics().getTotalRecordsRead());
            entry.setOutputRecords(1); // Audit record
            job.getLineage().add(entry);

            log.info("Auditor: DQ Score: {}, Compliance Score: {}", dqScore, complianceScore);

        } catch (Exception e) {
            log.error("Auditor: Error during audit", e);
            PipelineJob.ErrorRecord error = new PipelineJob.ErrorRecord(
                    UUID.randomUUID().toString(),
                    "AUDIT",
                    "AUDIT_ERROR",
                    e.getMessage()
            );
            job.getErrors().add(error);
            throw e;
        }

        long duration = System.currentTimeMillis() - startTime;
        job.getLineage().get(job.getLineage().size() - 1).setDurationMs(duration);
    }

    private double calculateDataQualityScore(PipelineJob job) {
        double baseScore = 100.0;

        // Deduct for null percentages
        baseScore -= job.getStatistics().getTotalRecordsRejected() * 0.1;

        // Deduct for duplicates
        baseScore -= job.getStatistics().getTotalRecordsDeduplicated() * 0.05;

        // Deduct for validation errors
        baseScore -= job.getErrors().size() * 0.5;

        return Math.max(0, Math.min(100, baseScore));
    }

    private double calculateComplianceScore(PipelineJob job) {
        double score = 100.0;

        // Check for required fields
        if (job.getTargetDataset() == null || job.getTargetTable() == null) {
            score -= 20;
        }

        // Check for lineage tracking
        if (job.getLineage().size() < 3) {
            score -= 15; // Incomplete lineage
        }

        // Check for error handling
        if (!job.getErrors().isEmpty()) {
            score -= 10;
        }

        // Check for audit log
        if (job.getAuditLogPath() == null) {
            score -= 10;
        }

        return Math.max(0, Math.min(100, score));
    }

    private String generateReasoningLog(PipelineJob job, double dqScore, double complianceScore) {
        StringBuilder log = new StringBuilder();

        log.append("=== ETL Pipeline Audit Report ===\n");
        log.append("Job ID: ").append(job.getJobId()).append("\n");
        log.append("Status: ").append(job.getStatus()).append("\n");
        log.append("Dataset Version: ").append(job.getDatasetVersion()).append("\n");
        log.append("Mapping Version: ").append(job.getMappingVersion()).append("\n\n");

        log.append("=== Data Quality Scorecard ===\n");
        log.append("Total Records Read: ").append(job.getStatistics().getTotalRecordsRead()).append("\n");
        log.append("Total Records Loaded: ").append(job.getStatistics().getTotalRecordsLoaded()).append("\n");
        log.append("Total Records Rejected: ").append(job.getStatistics().getTotalRecordsRejected()).append("\n");
        log.append("Total Records Deduplicated: ").append(job.getStatistics().getTotalRecordsDeduplicated()).append("\n");
        log.append("Data Quality Score: ").append(String.format("%.2f", dqScore)).append("/100\n\n");

        log.append("=== Compliance Report ===\n");
        log.append("Compliance Score: ").append(String.format("%.2f", complianceScore)).append("/100\n");
        log.append("Lineage Entries: ").append(job.getLineage().size()).append("\n");
        log.append("Error Count: ").append(job.getErrors().size()).append("\n\n");

        log.append("=== Lineage Tracking ===\n");
        for (PipelineJob.LineageEntry entry : job.getLineage()) {
            log.append(String.format("[%s] %s (Agent: %s, Duration: %dms)\n",
                    entry.getStep(),
                    entry.getStep(),
                    entry.getAgentName(),
                    entry.getDurationMs()
            ));
            log.append(String.format("  Input Records: %d, Output Records: %d\n",
                    entry.getInputRecords(),
                    entry.getOutputRecords()
            ));
        }

        if (!job.getErrors().isEmpty()) {
            log.append("\n=== Errors Encountered ===\n");
            for (PipelineJob.ErrorRecord error : job.getErrors()) {
                log.append(String.format("[%s] %s: %s\n",
                        error.getErrorType(),
                        error.getFieldName(),
                        error.getErrorMessage()
                ));
            }
        }

        return log.toString();
    }

    private void storeAuditResults(PipelineJob job, double dqScore, double complianceScore, String reasoningLog) {
        // In production, would store to BigQuery audit tables
        log.info("Audit Log:\n{}", reasoningLog);

        job.setAuditLogPath("gs://audit-logs/" + job.getJobId() + "/audit.log");
    }
}

