package com.etl.agent.agents;

import com.etl.agent.domain.*;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Cartographer Agent: Schema inference and type detection.
 * Responsibilities:
 * - Infers schema/semantics with confidence scores
 * - Detects schema drift vs prior version
 * - Outputs "Source Schema Contract" JSON
 */
@Slf4j
public class CartographerAgent implements ETLAgent {

    private static final Map<String, Pattern> TYPE_PATTERNS = new HashMap<>();

    static {
        TYPE_PATTERNS.put("INTEGER", Pattern.compile("^-?\\d+$"));
        TYPE_PATTERNS.put("FLOAT", Pattern.compile("^-?\\d+\\.\\d+$"));
        TYPE_PATTERNS.put("BOOLEAN", Pattern.compile("^(true|false|yes|no|0|1)$", Pattern.CASE_INSENSITIVE));
        TYPE_PATTERNS.put("DATE", Pattern.compile("^\\d{4}-\\d{2}-\\d{2}$"));
        TYPE_PATTERNS.put("TIMESTAMP", Pattern.compile("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}"));
        TYPE_PATTERNS.put("EMAIL", Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$"));
        TYPE_PATTERNS.put("UUID", Pattern.compile("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$", Pattern.CASE_INSENSITIVE));
    }

    @Override
    public String getName() {
        return "Cartographer";
    }

    @Override
    public void execute(PipelineJob job) throws Exception {
        log.info("Cartographer: Inferring schema from samples");
        long startTime = System.currentTimeMillis();

        try {
            // In real implementation, would load SchemaContract from GCS
            SchemaContract contract = createSampleContract();

            // Infer types for each field based on sample rows
            inferFieldTypes(contract);

            // Detect patterns and unique characteristics
            analyzeFieldCharacteristics(contract);

            // Store inferred schema
            job.setDatasetVersion("v1_" + System.currentTimeMillis());

            // Add lineage entry
            PipelineJob.LineageEntry entry = new PipelineJob.LineageEntry("SCHEMA_INFERENCE", "Cartographer");
            entry.setInputRecords(job.getStatistics().getTotalRecordsRead());
            entry.setOutputRecords(contract.getFields().size());
            job.getLineage().add(entry);

            job.setStatus(PipelineJob.JobStatus.SCHEMA_DISCOVERED);
            log.info("Cartographer: Discovered {} fields with type inference", contract.getFields().size());

        } catch (Exception e) {
            log.error("Cartographer: Error during schema inference", e);
            PipelineJob.ErrorRecord error = new PipelineJob.ErrorRecord(
                    UUID.randomUUID().toString(),
                    "SCHEMA_INFERENCE",
                    "INFERENCE_ERROR",
                    e.getMessage()
            );
            job.getErrors().add(error);
            job.setStatus(PipelineJob.JobStatus.FAILED);
            throw e;
        }

        long duration = System.currentTimeMillis() - startTime;
        job.getLineage().get(job.getLineage().size() - 1).setDurationMs(duration);
    }

    private void inferFieldTypes(SchemaContract contract) {
        for (SchemaContract.FieldContract field : contract.getFields()) {
            Map<String, Integer> typeCounts = new HashMap<>();

            for (Map<String, Object> sampleRow : contract.getSampleRows()) {
                Object value = sampleRow.get(field.getName());
                if (value == null || value.toString().isEmpty()) {
                    continue;
                }

                String inferredType = inferType(value.toString());
                typeCounts.put(inferredType, typeCounts.getOrDefault(inferredType, 0) + 1);
            }

            // Determine most likely type
            String bestType = "STRING";
            double maxConfidence = 0.0;

            for (Map.Entry<String, Integer> entry : typeCounts.entrySet()) {
                double confidence = (double) entry.getValue() / contract.getSampleRows().size();
                if (confidence > maxConfidence) {
                    maxConfidence = confidence;
                    bestType = entry.getKey();
                }
            }

            field.setInferredType(bestType);
            field.setConfidenceScore(Math.min(maxConfidence, 1.0));

            // Detect patterns
            detectPatterns(field, contract.getSampleRows());
        }
    }

    private String inferType(String value) {
        for (Map.Entry<String, Pattern> entry : TYPE_PATTERNS.entrySet()) {
            if (entry.getValue().matcher(value).matches()) {
                return entry.getKey();
            }
        }
        return "STRING";
    }

    private void analyzeFieldCharacteristics(SchemaContract contract) {
        for (SchemaContract.FieldContract field : contract.getFields()) {
            Set<String> uniqueValues = new HashSet<>();
            int nullCount = 0;

            for (Map<String, Object> row : contract.getSampleRows()) {
                Object value = row.get(field.getName());
                if (value == null || value.toString().isEmpty()) {
                    nullCount++;
                } else {
                    uniqueValues.add(value.toString());
                }
            }

            field.setNullPercentage((double) nullCount / contract.getSampleRows().size());
            field.setUnique(uniqueValues.size() == contract.getSampleRows().size() - nullCount);
            field.setSampleValues(new ArrayList<>(uniqueValues.stream().limit(5).toList()));
        }
    }

    private void detectPatterns(SchemaContract.FieldContract field, List<Map<String, Object>> sampleRows) {
        List<String> patterns = new ArrayList<>();

        // Example patterns: ID field, key field, date field, amount field
        String fieldNameLower = field.getName().toLowerCase();
        if (fieldNameLower.contains("id") || fieldNameLower.contains("key")) {
            patterns.add("KEY_FIELD");
            field.setUnique(true);
        }
        if (fieldNameLower.contains("date") || fieldNameLower.contains("time")) {
            patterns.add("TEMPORAL_FIELD");
        }
        if (fieldNameLower.contains("amount") || fieldNameLower.contains("value") || fieldNameLower.contains("price")) {
            patterns.add("NUMERIC_MEASURE");
        }
        if (fieldNameLower.contains("code") || fieldNameLower.contains("type")) {
            patterns.add("CATEGORICAL_FIELD");
        }

        field.setDetectedPatterns(patterns);
    }

    private SchemaContract createSampleContract() {
        SchemaContract contract = new SchemaContract();
        contract.setSchemaId(UUID.randomUUID().toString());
        contract.setVersion("1.0.0");

        // Sample fields
        SchemaContract.FieldContract idField = new SchemaContract.FieldContract("id", "STRING");
        SchemaContract.FieldContract nameField = new SchemaContract.FieldContract("name", "STRING");
        SchemaContract.FieldContract amountField = new SchemaContract.FieldContract("amount", "FLOAT");
        SchemaContract.FieldContract dateField = new SchemaContract.FieldContract("transaction_date", "DATE");

        contract.getFields().addAll(Arrays.asList(idField, nameField, amountField, dateField));

        // Sample rows
        Map<String, Object> row1 = new HashMap<>();
        row1.put("id", "12345");
        row1.put("name", "John Doe");
        row1.put("amount", "1500.50");
        row1.put("transaction_date", "2024-01-15");
        contract.getSampleRows().add(row1);

        Map<String, Object> row2 = new HashMap<>();
        row2.put("id", "12346");
        row2.put("name", "Jane Smith");
        row2.put("amount", "2000.75");
        row2.put("transaction_date", "2024-01-16");
        contract.getSampleRows().add(row2);

        return contract;
    }
}

