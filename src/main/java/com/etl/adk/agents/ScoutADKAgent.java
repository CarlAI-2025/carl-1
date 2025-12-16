package com.etl.adk.agents;

import com.etl.agent.adk.core.ADKBaseAgent;
import com.etl.agent.adk.core.VertexAIClientCore;
import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;

/**
 * Scout Agent - Ingestion and Validation
 * Formal name: Scout
 * Triggered by: Event (file drop)
 *
 * Responsibilities (from design.txt):
 * 1. Pull the dataset (file/API)
 * 2. Format checks + basic validation
 * 3. Generate stats/metrics
 * 4. Emit schema fingerprint + sample rows
 */
@Slf4j
public class ScoutADKAgent implements ADKBaseAgent {

    private final VertexAIClientCore vertexAI;
    private final Storage storage;

    public ScoutADKAgent(String projectId, String location) {
        this.vertexAI = new VertexAIClientCore(projectId, location, "gemini-pro");
        this.storage = StorageOptions.getDefaultInstance().getService();
    }

    @Override
    public String getFormalName() {
        return "Scout";
    }

    @Override
    public String getDescription() {
        return "Ingestion Agent: Pulls dataset from GCS, validates format, generates statistics and schema fingerprints";
    }

    @Override
    public void initialize() throws Exception {
        log.info("Initializing Scout Agent");
        vertexAI.initialize();
    }

    @Override
    public AgentResponse execute(AgentRequest request) throws Exception {
        long startTime = System.currentTimeMillis();
        log.info("Scout: Processing file: {}", request.getPayload());

        try {
            // Parse GCS path
            String[] parts = request.getPayload().split("/");
            String bucket = parts[0];
            String objectName = String.join("/", java.util.Arrays.copyOfRange(parts, 1, parts.length));

            // Read file from GCS
            String fileContent = readFromGCS(bucket, objectName);

            // Use Vertex AI to analyze file
            String analysisPrompt = String.format(
                    "Analyze this CSV/data file and provide:\n" +
                    "1. Format validation (CSV, JSON, Parquet)\n" +
                    "2. Row and column count\n" +
                    "3. Sample first 3 rows\n" +
                    "4. Data type inference for columns\n" +
                    "5. Quality metrics (nulls, duplicates)\n\n" +
                    "File content (first 1000 chars):\n%s",
                    fileContent.substring(0, Math.min(1000, fileContent.length()))
            );

            String analysis = vertexAI.generateContent(analysisPrompt);

            long duration = System.currentTimeMillis() - startTime;
            log.info("Scout: Analysis completed in {}ms", duration);

            return new AgentResponse(request.getJobId(), analysis, duration, true, null);

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("Scout: Error processing file", e);
            return new AgentResponse(request.getJobId(), null, duration, false, e.getMessage());
        }
    }

    @Override
    public void shutdown() throws Exception {
        log.info("Shutting down Scout Agent");
        vertexAI.shutdown();
    }

    /**
     * Read file from Google Cloud Storage
     */
    private String readFromGCS(String bucketName, String objectName) throws Exception {
        log.info("Reading from GCS: gs://{}/{}", bucketName, objectName);

        Bucket bucket = storage.get(bucketName);
        if (bucket == null) {
            throw new Exception("Bucket not found: " + bucketName);
        }

        var blob = bucket.get(objectName);
        if (blob == null) {
            throw new Exception("Object not found: " + objectName);
        }

        byte[] content = blob.getContent();
        return new String(content, StandardCharsets.UTF_8);
    }
}

