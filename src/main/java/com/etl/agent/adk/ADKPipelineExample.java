package com.etl.agent.adk;

import com.etl.agent.domain.PipelineJob;
import lombok.extern.slf4j.Slf4j;

/**
 * Example application demonstrating ADK integration.
 * Shows how to use Google's Agent Development Kit with Vertex AI.
 */
@Slf4j
public class ADKPipelineExample {

    public static void main(String[] args) throws Exception {
        // Configuration
        String projectId = System.getenv("GCP_PROJECT_ID");
        String location = "us-central1";

        if (projectId == null || projectId.isEmpty()) {
            log.error("GCP_PROJECT_ID environment variable not set");
            System.exit(1);
        }

        // Initialize ADK Conductor Agent with Vertex AI
        log.info("=== ADK ETL Pipeline Example ===");
        log.info("Initializing ADK Conductor with Vertex AI");

        ADKConductorAgent conductor = new ADKConductorAgent(projectId, location);

        try {
            // Step 1: Initialize ADK with Vertex AI
            log.info("\nStep 1: Initializing Vertex AI connection");
            conductor.initialize();
            log.info("✓ Vertex AI initialized successfully");

            // Step 2: Create pipeline job
            log.info("\nStep 2: Creating pipeline job");
            PipelineJob job = new PipelineJob();
            job.setSourcePath("gs://data-bucket/transactions.csv");
            job.setTargetDataset("etl_dataset");
            job.setTargetTable("transactions");
            job.getStatistics().setTotalRecordsRead(10000);
            job.getStatistics().setTotalRecordsLoaded(9850);
            job.getStatistics().setTotalRecordsRejected(150);
            log.info("✓ Pipeline job created: {}", job.getJobId());

            // Step 3: Use Vertex AI to analyze pipeline
            log.info("\nStep 3: Using Vertex AI to analyze pipeline");
            log.info("Sending pipeline data to Gemini for analysis...");
            conductor.analyzeAndOptimizePipeline(job);
            log.info("✓ Pipeline analysis completed by Vertex AI");

            // Step 4: Get AI transformation suggestions
            log.info("\nStep 4: Getting AI transformation suggestions");
            String sourceSchema = "{" +
                    "  'id': 'STRING'," +
                    "  'date': 'STRING'," +
                    "  'amount': 'STRING'" +
                    "}";
            String targetSchema = "{" +
                    "  'security_id': 'STRING'," +
                    "  'transaction_date': 'DATE'," +
                    "  'transaction_amount': 'NUMERIC'" +
                    "}";

            log.info("Requesting Vertex AI to suggest transformations...");
            String suggestions = conductor.suggestTransformations(sourceSchema, targetSchema);
            log.info("AI Transformation Suggestions:\n{}", suggestions);

            // Step 5: Analyze data quality with AI
            log.info("\nStep 5: Analyzing data quality with Vertex AI");
            String dataProfile = "{" +
                    "  'total_records': 10000," +
                    "  'null_percentage': 1.5," +
                    "  'duplicate_percentage': 0.8," +
                    "  'outlier_percentage': 2.1" +
                    "}";

            log.info("Sending data profile to Vertex AI for quality analysis...");
            String qualityAnalysis = conductor.analyzeDataQuality(dataProfile);
            log.info("Data Quality Analysis:\n{}", qualityAnalysis);

            // Step 6: Stream real-time optimization feedback
            log.info("\nStep 6: Streaming real-time optimization feedback");
            log.info("Streaming pipeline optimization analysis...");
            conductor.streamPipelineOptimization(job);
            log.info("✓ Real-time streaming completed");

            // Step 7: Initialize and display tool registry
            log.info("\nStep 7: Available ADK Tools");
            ADKToolRegistry toolRegistry = new ADKToolRegistry();
            toolRegistry.printSummary();

            log.info("\n=== ADK Pipeline Example Complete ===");
            log.info("Successfully demonstrated ADK integration with Vertex AI");

        } catch (Exception e) {
            log.error("Error in ADK pipeline example", e);
            throw e;
        } finally {
            // Step 8: Cleanup
            log.info("\nStep 8: Shutting down ADK Conductor");
            conductor.shutdown();
            log.info("✓ ADK Conductor shut down successfully");
        }
    }

    /**
     * Alternative example: Using ADK with tool registry
     */
    public static void exampleWithToolRegistry() throws Exception {
        log.info("=== ADK Tool Registry Example ===");

        // Create tool registry
        ADKToolRegistry toolRegistry = new ADKToolRegistry();

        // Display available tools
        log.info("Available Tools in Registry:");
        for (ADKToolRegistry.ADKTool tool : toolRegistry.getAllTools()) {
            log.info("  - {} ({}): {}",
                    tool.getName(),
                    tool.getParameters().length + " params",
                    tool.getDescription());
        }

        // Get specific tool
        ADKToolRegistry.ADKTool validateSchemaTool = toolRegistry.getTool("validateSchema");
        log.info("\nSelected Tool: {}", validateSchemaTool);

        log.info("Total Tools: {}", toolRegistry.getToolCount());
    }

    /**
     * Example: Custom ADK agent implementation
     */
    static class CustomADKAgent implements ADKAgent {
        private final String name;
        private final VertexAIClient vertexAIClient;

        public CustomADKAgent(String name, String projectId, String location) {
            this.name = name;
            this.vertexAIClient = new VertexAIClient(projectId, location, "gemini-pro");
        }

        @Override
        public void initialize() throws Exception {
            log.info("Initializing custom agent: {}", name);
            vertexAIClient.initialize();
        }

        @Override
        public com.google.cloud.vertexai.api.GenerateContentResponse executeWithAI(
                com.google.cloud.vertexai.api.GenerateContentRequest request) throws Exception {
            log.info("Custom agent {} executing with Vertex AI", name);

            // Custom implementation would go here
            // For now, just logging the execution
            return null;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getCapabilities() {
            return String.format("Custom ADK Agent: %s with Vertex AI capabilities", name);
        }

        @Override
        public void shutdown() throws Exception {
            log.info("Shutting down custom agent: {}", name);
            vertexAIClient.shutdown();
        }
    }
}

