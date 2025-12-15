package com.etl.agent.adk;

import com.etl.agent.adk.agents.ConductorADKAgent;
import com.etl.agent.adk.core.ADKBaseAgent;
import lombok.extern.slf4j.Slf4j;
import java.util.UUID;

/**
 * Main Entry Point for Pure ADK-Based ETL Pipeline
 *
 * This is a clean, production-ready implementation using:
 * - Google ADK (Agent Development Kit)
 * - Vertex AI (Gemini Pro)
 * - BigQuery
 * - Google Cloud Storage
 * - Dataflow/Beam (optional scaling)
 *
 * Architecture follows design.txt specifications:
 * Conductor (Root) -> Scout -> Cartographer -> Navigator -> Alchemist -> Architect -> Auditor
 */
@Slf4j
public class ADKPipelineMain {

    public static void main(String[] args) throws Exception {
        log.info("========================================");
        log.info("ADK-Based ETL Pipeline - By Carl Gauss");
        log.info("========================================");

        // Configuration
        String projectId = System.getenv("GCP_PROJECT_ID");
        String location = System.getenv("GCP_LOCATION");
        String gcsBucket = System.getenv("GCS_BUCKET");
        String gcsObject = System.getenv("GCS_OBJECT");

        // Defaults
        if (projectId == null || projectId.isEmpty()) {
            log.error("GCP_PROJECT_ID environment variable not set");
            System.exit(1);
        }
        if (location == null || location.isEmpty()) {
            location = "us-central1";
        }
        if (gcsBucket == null || gcsBucket.isEmpty()) {
            gcsBucket = projectId + "-etl-data";
        }
        if (gcsObject == null || gcsObject.isEmpty()) {
            gcsObject = "data/input.csv";
        }

        log.info("Configuration:");
        log.info("  Project ID: {}", projectId);
        log.info("  Location: {}", location);
        log.info("  GCS Bucket: {}", gcsBucket);
        log.info("  GCS Object: {}", gcsObject);

        // Create pipeline job
        String jobId = "ETL_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        log.info("\nStarting ETL Pipeline - Job ID: {}", jobId);

        // Initialize Conductor (Root Orchestrator)
        ConductorADKAgent conductor = new ConductorADKAgent(projectId, location);

        try {
            // Initialize all agents
            conductor.initialize();

            // Create pipeline request
            String sourceFile = gcsBucket + "/" + gcsObject;
            ADKBaseAgent.AgentRequest pipelineRequest = new ADKBaseAgent.AgentRequest(
                    jobId,
                    sourceFile,
                    "text/csv"
            );

            // Execute full ETL pipeline
            log.info("\n▶ Starting ETL Execution...\n");
            long startTime = System.currentTimeMillis();

            ADKBaseAgent.AgentResponse response = conductor.execute(pipelineRequest);

            long duration = System.currentTimeMillis() - startTime;

            // Print results
            log.info("\n" + "=".repeat(50));
            if (response.isSuccess()) {
                log.info("✓ ETL PIPELINE COMPLETED SUCCESSFULLY");
                log.info("  Duration: {}ms ({} seconds)", duration, duration / 1000);
                log.info("  Job ID: {}", response.getJobId());
                log.info("\nFinal Report:");
                log.info(response.getResult());
            } else {
                log.error("✗ ETL PIPELINE FAILED");
                log.error("  Error: {}", response.getErrorMessage());
            }
            log.info("=".repeat(50) + "\n");

        } catch (Exception e) {
            log.error("Fatal error in ETL pipeline", e);
            System.exit(1);
        } finally {
            try {
                conductor.shutdown();
                log.info("✓ Pipeline shutdown complete");
            } catch (Exception e) {
                log.error("Error during shutdown", e);
            }
        }
    }
}

