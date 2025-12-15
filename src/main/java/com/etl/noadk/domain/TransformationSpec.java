package com.etl.noadk.domain;

import com.google.gson.annotations.SerializedName;
import java.util.*;

/**
 * Transformation rules specification produced by Alchemist agent.
 */
public class TransformationSpec {
    @SerializedName("spec_id")
    private String specId;

    @SerializedName("version")
    private String version;

    @SerializedName("timestamp")
    private long timestamp;

    @SerializedName("cleaning_rules")
    private List<CleaningRule> cleaningRules;

    @SerializedName("enrichment_steps")
    private List<EnrichmentStep> enrichmentSteps;

    @SerializedName("deduplication_config")
    private DeduplicationConfig deduplicationConfig;

    @SerializedName("aggregations")
    private List<AggregationRule> aggregations;

    public TransformationSpec() {
        this.cleaningRules = new ArrayList<>();
        this.enrichmentSteps = new ArrayList<>();
        this.aggregations = new ArrayList<>();
        this.timestamp = System.currentTimeMillis();
    }

    public static class CleaningRule {
        @SerializedName("field")
        private String field;

        @SerializedName("operations")
        private List<String> operations;

        @SerializedName("null_handling")
        private String nullHandling;

        public CleaningRule(String field) {
            this.field = field;
            this.operations = new ArrayList<>();
            this.nullHandling = "skip";
        }

        public String getField() { return field; }
        public List<String> getOperations() { return operations; }
        public String getNullHandling() { return nullHandling; }
        public void setNullHandling(String nullHandling) { this.nullHandling = nullHandling; }
    }

    public static class EnrichmentStep {
        @SerializedName("name")
        private String name;

        @SerializedName("source_table")
        private String sourceTable;

        @SerializedName("join_key")
        private String joinKey;

        @SerializedName("target_fields")
        private List<String> targetFields;

        public EnrichmentStep(String name, String sourceTable) {
            this.name = name;
            this.sourceTable = sourceTable;
            this.targetFields = new ArrayList<>();
        }

        public String getName() { return name; }
        public String getSourceTable() { return sourceTable; }
        public String getJoinKey() { return joinKey; }
        public void setJoinKey(String joinKey) { this.joinKey = joinKey; }
        public List<String> getTargetFields() { return targetFields; }
    }

    public static class DeduplicationConfig {
        @SerializedName("enabled")
        private boolean enabled = true;

        @SerializedName("key_fields")
        private List<String> keyFields;

        @SerializedName("survivorship_rules")
        private Map<String, String> survivorshipRules;

        public DeduplicationConfig() {
            this.keyFields = new ArrayList<>();
            this.survivorshipRules = new HashMap<>();
        }

        public boolean isEnabled() { return enabled; }
        public List<String> getKeyFields() { return keyFields; }
        public Map<String, String> getSurvivorshipRules() { return survivorshipRules; }
    }

    public static class AggregationRule {
        @SerializedName("measure_field")
        private String measureField;

        @SerializedName("aggregation_type")
        private String aggregationType;

        @SerializedName("group_by_fields")
        private List<String> groupByFields;

        @SerializedName("target_field_name")
        private String targetFieldName;

        public AggregationRule(String measureField, String aggregationType) {
            this.measureField = measureField;
            this.aggregationType = aggregationType;
            this.groupByFields = new ArrayList<>();
        }

        public String getMeasureField() { return measureField; }
        public String getAggregationType() { return aggregationType; }
        public List<String> getGroupByFields() { return groupByFields; }
        public String getTargetFieldName() { return targetFieldName; }
        public void setTargetFieldName(String targetFieldName) { this.targetFieldName = targetFieldName; }
    }

    // Getters and setters
    public String getSpecId() { return specId; }
    public void setSpecId(String specId) { this.specId = specId; }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public long getTimestamp() { return timestamp; }

    public List<CleaningRule> getCleaningRules() { return cleaningRules; }
    public List<EnrichmentStep> getEnrichmentSteps() { return enrichmentSteps; }
    public DeduplicationConfig getDeduplicationConfig() { return deduplicationConfig; }
    public void setDeduplicationConfig(DeduplicationConfig config) { this.deduplicationConfig = config; }
    public List<AggregationRule> getAggregations() { return aggregations; }
}

