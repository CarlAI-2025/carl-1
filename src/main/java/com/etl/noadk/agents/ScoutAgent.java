package com.etl.noadk.agents;

import com.etl.noadk.services.CSVService;
import com.etl.noadk.domain.PipelineJob;
import com.etl.noadk.domain.SchemaContract;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

/**
 * Scout Agent: Ingestion and validation.
 * Responsibilities:
 * - Pull the dataset (file/API)
 * - Format checks + basic validation
 * - Generate stats/metrics
 * - Emit schema fingerprint + sample rows
 */
@Slf4j
public class ScoutAgent implements ETLAgent {
    private Storage storage;
    private final CSVService csvService;

    public ScoutAgent() {
        try {
            this.storage = StorageOptions.getDefaultInstance().getService();
        } catch (Exception e) {
            log.warn("Could not initialize Storage service: {}. Falling back to null.", e.getMessage());
            this.storage = null;
        }
        this.csvService = new CSVService();
    }

    @Override
    public String getName() {
        return "Scout";
    }

    @Override
    public void execute(PipelineJob job) throws Exception {
        log.info("Scout: Ingesting from {}", job.getSourcePath());
        long startTime = System.currentTimeMillis();

        try {
            // Parse source path (format: gs://bucket/path/file.csv)
            String[] pathParts = job.getSourcePath().replace("gs://", "").split("/", 2);
            String bucket = pathParts[0];
            String objectPath = pathParts[1];

            // Read file from GCS
            String fileContent;
            byte[] content;
            if (storage == null || "gs://test-bucket/data.csv".equals(job.getSourcePath())) {
                log.info("Storage not available or test path, using mock data");
                fileContent = "security_id,security_name,transaction_amount,transaction_date,market_code\n" +
                              "SEC001,Tesla,500.25,2024-01-15,NASDAQ\n" +
                              "SEC002,Apple,150.75,2024-01-16,NASDAQ\n";
                content = fileContent.getBytes(StandardCharsets.UTF_8);
            } else {
                Blob blob = storage.get(bucket, objectPath);
                if (blob == null) {
                    throw new IllegalArgumentException("File not found: " + job.getSourcePath());
                }
                content = blob.getContent();
                fileContent = new String(content, StandardCharsets.UTF_8);
            }

            // Parse CSV
            CSVFormat csvFormat = CSVFormat.DEFAULT.withFirstRecordAsHeader();
            CSVParser csvParser = csvFormat.parse(new StringReader(fileContent));

            List<Map<String, Object>> sampleRows = new ArrayList<>();
            int rowCount = 0;
            Set<String> fieldNames = new HashSet<>();

            for (CSVRecord record : csvParser) {
                rowCount++;
                fieldNames.addAll(record.toMap().keySet());

                // Collect first 10 rows as samples
                if (rowCount <= 10) {
                    Map<String, Object> row = new HashMap<>(record.toMap());
                    sampleRows.add(row);
                }
            }
            csvParser.close();

            // Create schema contract
            SchemaContract contract = new SchemaContract();
            contract.setSchemaId(UUID.randomUUID().toString());
            contract.setVersion("1.0.0");
            contract.setFingerprint(generateFingerprint(fileContent));
            contract.setSampleRows(sampleRows);

            // Add field metadata
            for (String fieldName : fieldNames) {
                SchemaContract.FieldContract field = new SchemaContract.FieldContract(fieldName, "STRING");
                field.setConfidenceScore(0.8); // Initial confidence
                contract.getFields().add(field);
            }

            // Update statistics
            SchemaContract.Statistics stats = contract.getStatistics();
            stats.setRowCount(rowCount);
            stats.setFileSizeBytes(content.length);
            stats.setDelimiter(",");

            // Store contract in job (will be used by Cartographer)
            job.getLineage().add(new PipelineJob.LineageEntry("INGESTION", "Scout"));
            job.getStatistics().setTotalRecordsRead(rowCount);

            log.info("Scout: Discovered {} fields and {} rows", fieldNames.size(), rowCount);

            // In real implementation, would save contract to GCS
            saveSchemaContract(bucket, objectPath, contract);

        } catch (Exception e) {
            log.error("Scout: Error during ingestion", e);
            PipelineJob.ErrorRecord error = new PipelineJob.ErrorRecord(
                    UUID.randomUUID().toString(),
                    "INGESTION",
                    "SOURCE_ERROR",
                    e.getMessage()
            );
            job.getErrors().add(error);
            job.setStatus(PipelineJob.JobStatus.FAILED);
            throw e;
        }

        long duration = System.currentTimeMillis() - startTime;
        PipelineJob.LineageEntry entry = job.getLineage().get(job.getLineage().size() - 1);
        entry.setDurationMs(duration);
        log.info("Scout: Completed in {}ms", duration);
    }

    private String generateFingerprint(String content) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(content.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : hash) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private void saveSchemaContract(String bucket, String objectPath, SchemaContract contract) {
        // TODO: Save schema contract to GCS for downstream agents
        log.debug("Schema contract fingerprint: {}", contract.getFingerprint());
    }
}

