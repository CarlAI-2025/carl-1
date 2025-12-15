package com.etl.noadk.domain;

import com.google.gson.annotations.SerializedName;
import java.util.*;

/**
 * Pipeline job state and execution lineage.
 */
public class PipelineJob {
    @SerializedName("job_id")
    private String jobId;

    @SerializedName("status")
    private JobStatus status;

    @SerializedName("created_at")
    private long createdAt;

    @SerializedName("started_at")
    private Long startedAt;

    @SerializedName("completed_at")
    private Long completedAt;

    @SerializedName("dataset_version")
    private String datasetVersion;

    @SerializedName("mapping_version")
    private String mappingVersion;

    @SerializedName("source_path")
    private String sourcePath;

    @SerializedName("target_dataset")
    private String targetDataset;

    @SerializedName("target_table")
    private String targetTable;

    @SerializedName("lineage")
    private List<LineageEntry> lineage;

    @SerializedName("errors")
    private List<ErrorRecord> errors;

    @SerializedName("statistics")
    private JobStatistics statistics;

    @SerializedName("audit_log")
    private String auditLogPath;

    public enum JobStatus {
        INITIATED,
        SCHEMA_DISCOVERED,
        MAPPED,
        TRANSFORMED,
        VALIDATED,
        LOADED,
        COMPLETED,
        FAILED,
        ROLLED_BACK
    }

    public PipelineJob() {
        this.jobId = UUID.randomUUID().toString();
        this.status = JobStatus.INITIATED;
        this.createdAt = System.currentTimeMillis();
        this.lineage = new ArrayList<>();
        this.errors = new ArrayList<>();
        this.statistics = new JobStatistics();
    }

    public static class LineageEntry {
        @SerializedName("step")
        private String step;

        @SerializedName("timestamp")
        private long timestamp;

        @SerializedName("input_records")
        private long inputRecords;

        @SerializedName("output_records")
        private long outputRecords;

        @SerializedName("agent_name")
        private String agentName;

        @SerializedName("duration_ms")
        private long durationMs;

        public LineageEntry(String step, String agentName) {
            this.step = step;
            this.agentName = agentName;
            this.timestamp = System.currentTimeMillis();
        }

        // Getters and setters
        public String getStep() { return step; }
        public long getTimestamp() { return timestamp; }
        public long getInputRecords() { return inputRecords; }
        public void setInputRecords(long inputRecords) { this.inputRecords = inputRecords; }
        public long getOutputRecords() { return outputRecords; }
        public void setOutputRecords(long outputRecords) { this.outputRecords = outputRecords; }
        public String getAgentName() { return agentName; }
        public long getDurationMs() { return durationMs; }
        public void setDurationMs(long durationMs) { this.durationMs = durationMs; }
    }

    public static class ErrorRecord {
        @SerializedName("record_id")
        private String recordId;

        @SerializedName("field_name")
        private String fieldName;

        @SerializedName("error_type")
        private String errorType;

        @SerializedName("error_message")
        private String errorMessage;

        @SerializedName("raw_value")
        private String rawValue;

        @SerializedName("timestamp")
        private long timestamp;

        public ErrorRecord(String recordId, String fieldName, String errorType, String errorMessage) {
            this.recordId = recordId;
            this.fieldName = fieldName;
            this.errorType = errorType;
            this.errorMessage = errorMessage;
            this.timestamp = System.currentTimeMillis();
        }

        // Getters and setters
        public String getRecordId() { return recordId; }
        public String getFieldName() { return fieldName; }
        public String getErrorType() { return errorType; }
        public String getErrorMessage() { return errorMessage; }
        public String getRawValue() { return rawValue; }
        public void setRawValue(String rawValue) { this.rawValue = rawValue; }
        public long getTimestamp() { return timestamp; }
    }

    public static class JobStatistics {
        @SerializedName("total_records_read")
        private long totalRecordsRead;

        @SerializedName("total_records_loaded")
        private long totalRecordsLoaded;

        @SerializedName("total_records_rejected")
        private long totalRecordsRejected;

        @SerializedName("total_records_deduplicated")
        private long totalRecordsDeduplicated;

        public long getTotalRecordsRead() { return totalRecordsRead; }
        public void setTotalRecordsRead(long totalRecordsRead) { this.totalRecordsRead = totalRecordsRead; }

        public long getTotalRecordsLoaded() { return totalRecordsLoaded; }
        public void setTotalRecordsLoaded(long totalRecordsLoaded) { this.totalRecordsLoaded = totalRecordsLoaded; }

        public long getTotalRecordsRejected() { return totalRecordsRejected; }
        public void setTotalRecordsRejected(long totalRecordsRejected) { this.totalRecordsRejected = totalRecordsRejected; }

        public long getTotalRecordsDeduplicated() { return totalRecordsDeduplicated; }
        public void setTotalRecordsDeduplicated(long totalRecordsDeduplicated) { this.totalRecordsDeduplicated = totalRecordsDeduplicated; }
    }

    // Getters and setters
    public String getJobId() { return jobId; }

    public JobStatus getStatus() { return status; }
    public void setStatus(JobStatus status) { this.status = status; }

    public long getCreatedAt() { return createdAt; }

    public Long getStartedAt() { return startedAt; }
    public void setStartedAt(Long startedAt) { this.startedAt = startedAt; }

    public Long getCompletedAt() { return completedAt; }
    public void setCompletedAt(Long completedAt) { this.completedAt = completedAt; }

    public String getDatasetVersion() { return datasetVersion; }
    public void setDatasetVersion(String datasetVersion) { this.datasetVersion = datasetVersion; }

    public String getMappingVersion() { return mappingVersion; }
    public void setMappingVersion(String mappingVersion) { this.mappingVersion = mappingVersion; }

    public String getSourcePath() { return sourcePath; }
    public void setSourcePath(String sourcePath) { this.sourcePath = sourcePath; }

    public String getTargetDataset() { return targetDataset; }
    public void setTargetDataset(String targetDataset) { this.targetDataset = targetDataset; }

    public String getTargetTable() { return targetTable; }
    public void setTargetTable(String targetTable) { this.targetTable = targetTable; }

    public List<LineageEntry> getLineage() { return lineage; }

    public List<ErrorRecord> getErrors() { return errors; }

    public JobStatistics getStatistics() { return statistics; }

    public String getAuditLogPath() { return auditLogPath; }
    public void setAuditLogPath(String auditLogPath) { this.auditLogPath = auditLogPath; }
}

