package com.etl.noadk.services;

import com.google.cloud.bigquery.*;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * Service for BigQuery operations including schema creation, data loading, and idempotency checks.
 */
@Slf4j
public class BigQueryService {
    private final BigQuery bigQuery;

    public BigQueryService() {
        this.bigQuery = BigQueryOptions.getDefaultInstance().getService();
    }

    /**
     * Create dataset if not exists.
     */
    public void createDatasetIfNotExists(String datasetId) throws Exception {
        try {
            DatasetInfo datasetInfo = DatasetInfo.newBuilder(datasetId)
                    .setDescription("ETL Pipeline Dataset")
                    .setLocation("US")
                    .build();

            bigQuery.create(datasetInfo);
            log.info("Created dataset: {}", datasetId);
        } catch (BigQueryException e) {
            if (e.getMessage().contains("Already Exists")) {
                log.info("Dataset {} already exists", datasetId);
            } else {
                throw e;
            }
        }
    }

    /**
     * Create table with schema if not exists.
     */
    public void createTableIfNotExists(String datasetId, String tableId, Schema schema) throws Exception {
        try {
            TableId tableIdentifier = TableId.of(datasetId, tableId);
            TableInfo tableInfo = TableInfo.newBuilder(tableIdentifier, StandardTableDefinition.of(schema))
                    .setDescription("ETL loaded table")
                    .build();

            bigQuery.create(tableInfo);
            log.info("Created table: {}.{}", datasetId, tableId);
        } catch (BigQueryException e) {
            if (e.getMessage().contains("Already Exists")) {
                log.info("Table {}.{} already exists", datasetId, tableId);
            } else {
                throw e;
            }
        }
    }

    /**
     * Create error staging table for validation failures.
     */
    public void createStagingErrorTable(String datasetId) throws Exception {
        List<Field> fields = Arrays.asList(
                Field.of("error_id", StandardSQLTypeName.STRING),
                Field.of("record_id", StandardSQLTypeName.STRING),
                Field.of("field_name", StandardSQLTypeName.STRING),
                Field.of("error_type", StandardSQLTypeName.STRING),
                Field.of("error_message", StandardSQLTypeName.STRING),
                Field.of("raw_value", StandardSQLTypeName.STRING),
                Field.of("timestamp", StandardSQLTypeName.TIMESTAMP),
                Field.of("job_id", StandardSQLTypeName.STRING)
        );

        Schema schema = Schema.of(fields);
        createTableIfNotExists(datasetId, "staging_errors", schema);
    }

    /**
     * Create lineage tracking table.
     */
    public void createLineageTable(String datasetId) throws Exception {
        List<Field> fields = Arrays.asList(
                Field.of("job_id", StandardSQLTypeName.STRING),
                Field.of("target_table", StandardSQLTypeName.STRING),
                Field.of("execution_time", StandardSQLTypeName.TIMESTAMP),
                Field.of("records_loaded", StandardSQLTypeName.INT64),
                Field.of("dataset_version", StandardSQLTypeName.STRING),
                Field.of("mapping_version", StandardSQLTypeName.STRING),
                Field.of("is_idempotent_load", StandardSQLTypeName.BOOL)
        );

        Schema schema = Schema.of(fields);
        createTableIfNotExists(datasetId, "job_lineage", schema);
    }

    /**
     * Check if job ID already exists (for idempotency).
     */
    public boolean jobAlreadyExecuted(String datasetId, String jobId) throws Exception {
        String query = String.format(
                "SELECT COUNT(*) as count FROM `%s.job_lineage` WHERE job_id = '%s'",
                datasetId, jobId
        );

        QueryJobConfiguration queryConfig = QueryJobConfiguration.newBuilder(query).build();
        TableResult result = bigQuery.query(queryConfig);

        for (FieldValueList row : result.iterateAll()) {
            return row.get("count").getLongValue() > 0;
        }
        return false;
    }

    /**
     * Execute SQL query and return results.
     */
    public TableResult executeSql(String query) throws Exception {
        log.info("Executing SQL query");
        QueryJobConfiguration queryConfig = QueryJobConfiguration.newBuilder(query).build();
        return bigQuery.query(queryConfig);
    }

    /**
     * Load data from GCS CSV to BigQuery table.
     */
    public void loadCsvToTable(String gcsPath, String datasetId, String tableId,
                               FormatOptions format) throws Exception {
        LoadJobConfiguration loadConfig = LoadJobConfiguration.newBuilder(
                TableId.of(datasetId, tableId),
                gcsPath
        )
                .setFormatOptions(format)
                .setAutodetect(true)
                .setMaxBadRecords(100)
                .build();

        Job loadJob = bigQuery.create(JobInfo.newBuilder(loadConfig).build());
        loadJob.waitFor();

        if (loadJob.getStatus().getError() != null) {
            throw new Exception("Load job failed: " + loadJob.getStatus().getError());
        }

        log.info("Successfully loaded data from {} to {}.{}", gcsPath, datasetId, tableId);
    }

    /**
     * Create schema from field definitions.
     */
    public Schema createSchema(Map<String, String> fieldTypes) {
        List<Field> fields = new ArrayList<>();
        for (Map.Entry<String, String> entry : fieldTypes.entrySet()) {
            StandardSQLTypeName sqlType = mapToSQLType(entry.getValue());
            fields.add(Field.of(entry.getKey(), sqlType));
        }
        return Schema.of(fields);
    }

    private StandardSQLTypeName mapToSQLType(String type) {
        return switch (type.toUpperCase()) {
            case "INTEGER", "INT" -> StandardSQLTypeName.INT64;
            case "FLOAT", "DOUBLE", "NUMERIC" -> StandardSQLTypeName.NUMERIC;
            case "BOOLEAN", "BOOL" -> StandardSQLTypeName.BOOL;
            case "DATE" -> StandardSQLTypeName.DATE;
            case "TIMESTAMP" -> StandardSQLTypeName.TIMESTAMP;
            default -> StandardSQLTypeName.STRING;
        };
    }

    /**
     * Create table with sample schema for demonstration.
     */
    public void createSampleSecurityTable(String datasetId, String tableId) throws Exception {
        List<Field> fields = Arrays.asList(
                Field.of("security_id", StandardSQLTypeName.STRING),
                Field.of("security_name", StandardSQLTypeName.STRING),
                Field.of("transaction_amount", StandardSQLTypeName.NUMERIC),
                Field.of("transaction_date", StandardSQLTypeName.DATE),
                Field.of("market_code", StandardSQLTypeName.STRING),
                Field.of("market_name", StandardSQLTypeName.STRING),
                Field.of("load_timestamp", StandardSQLTypeName.TIMESTAMP),
                Field.of("job_id", StandardSQLTypeName.STRING)
        );

        Schema schema = Schema.of(fields);
        createTableIfNotExists(datasetId, tableId, schema);
    }
}

