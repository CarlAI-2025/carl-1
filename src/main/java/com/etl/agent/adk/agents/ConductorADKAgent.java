package com.etl.agent.adk.agents;

import com.etl.agent.adk.core.ADKBaseAgent;
import com.etl.agent.adk.core.VertexAIClientCore;
import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.BigQueryOptions;
import com.google.cloud.bigquery.TableId;
import lombok.extern.slf4j.Slf4j;
import java.util.*;

/**
 * Conductor Agent - Orchestrator (ADK Root Agent)
 * Formal name: Conductor
 * Triggered by: User request
 *
 * Responsibilities (from design.txt):
 * 1. Owns end-to-end workflow
 * 2. Routes tasks
 * 3. Maintains job state
 * 4. Handles retries/failures
 * 5. Enforces ordering
 * 6. Decides when to request human approval
 * 7. Maintains run/version lineage (dataset_version, mapping_version)
 */
@Slf4j
public class ConductorADKAgent implements ADKBaseAgent {

    private final String projectId;
    private final String location;
    private final BigQuery bigQuery;
    private final VertexAIClientCore vertexAI;

    private final ScoutADKAgent scout;
    private final CartographerADKAgent cartographer;
    private final NavigatorADKAgent navigator;
    private final AlchemistADKAgent alchemist;
    private final ArchitectADKAgent architect;
    private final AuditorADKAgent auditor;

    private final Map<String, String> jobState = new HashMap<>();
    private final List<String> executionHistory = new ArrayList<>();

    public ConductorADKAgent(String projectId, String location) {
        this.projectId = projectId;
        this.location = location;
        this.bigQuery = BigQueryOptions.getDefaultInstance().getService();
        this.vertexAI = new VertexAIClientCore(projectId, location, "gemini-pro");

        // Initialize all agents
        this.scout = new ScoutADKAgent(projectId, location);
        this.cartographer = new CartographerADKAgent(projectId, location);
        this.navigator = new NavigatorADKAgent(projectId, location);
        this.alchemist = new AlchemistADKAgent(projectId, location);
        this.architect = new ArchitectADKAgent(projectId, location);
        this.auditor = new AuditorADKAgent(projectId, location);
    }

    @Override
    public String getFormalName() {
        return "Conductor";
    }

    @Override
    public String getDescription() {
        return "Orchestrator: Owns end-to-end workflow, routes tasks, maintains job state, handles retries, enforces ordering, maintains lineage";
    }

    @Override
    public void initialize() throws Exception {
        log.info("Initializing Conductor Agent (Root Orchestrator)");
        vertexAI.initialize();

        scout.initialize();
        cartographer.initialize();
        navigator.initialize();
        alchemist.initialize();
        architect.initialize();
        auditor.initialize();

        log.info("✓ All agents initialized");
    }

    @Override
    public AgentResponse execute(AgentRequest request) throws Exception {
        long startTime = System.currentTimeMillis();
        String jobId = request.getJobId();

        log.info("Conductor: Starting ETL pipeline for job: {}", jobId);

        try {
            // 1. Scout - Ingestion
            log.info("Conductor: [1/6] Triggering Scout Agent");
            ADKBaseAgent.AgentResponse scoutResponse = executeAgentWithRetry(scout, request);
            if (!scoutResponse.isSuccess()) {
                return failedResponse(jobId, "Scout failed", startTime);
            }
            jobState.put(jobId + "_scout", "COMPLETED");

            // 2. Cartographer - Schema Inference
            log.info("Conductor: [2/6] Triggering Cartographer Agent");
            ADKBaseAgent.AgentRequest cartRequest = new ADKBaseAgent.AgentRequest(
                    jobId, scoutResponse.getResult(), "application/json"
            );
            ADKBaseAgent.AgentResponse cartResponse = executeAgentWithRetry(cartographer, cartRequest);
            if (!cartResponse.isSuccess()) {
                return failedResponse(jobId, "Cartographer failed", startTime);
            }
            jobState.put(jobId + "_cartographer", "COMPLETED");

            // 3. Navigator - Field Mapping
            log.info("Conductor: [3/6] Triggering Navigator Agent");
            ADKBaseAgent.AgentRequest navRequest = new ADKBaseAgent.AgentRequest(
                    jobId, cartResponse.getResult(), "application/json"
            );
            ADKBaseAgent.AgentResponse navResponse = executeAgentWithRetry(navigator, navRequest);
            if (!navResponse.isSuccess()) {
                return failedResponse(jobId, "Navigator failed", startTime);
            }
            jobState.put(jobId + "_navigator", "COMPLETED");

            // 4. Alchemist - Transformation Rules
            log.info("Conductor: [4/6] Triggering Alchemist Agent");
            ADKBaseAgent.AgentRequest alcRequest = new ADKBaseAgent.AgentRequest(
                    jobId, navResponse.getResult(), "application/json"
            );
            ADKBaseAgent.AgentResponse alcResponse = executeAgentWithRetry(alchemist, alcRequest);
            if (!alcResponse.isSuccess()) {
                return failedResponse(jobId, "Alchemist failed", startTime);
            }
            jobState.put(jobId + "_alchemist", "COMPLETED");

            // 5. Architect - SQL Generation
            log.info("Conductor: [5/6] Triggering Architect Agent");
            ADKBaseAgent.AgentRequest archRequest = new ADKBaseAgent.AgentRequest(
                    jobId, alcResponse.getResult(), "application/json"
            );
            ADKBaseAgent.AgentResponse archResponse = executeAgentWithRetry(architect, archRequest);
            if (!archResponse.isSuccess()) {
                return failedResponse(jobId, "Architect failed", startTime);
            }
            jobState.put(jobId + "_architect", "COMPLETED");

            // 6. Auditor - Quality Assessment
            log.info("Conductor: [6/6] Triggering Auditor Agent");
            ADKBaseAgent.AgentRequest audRequest = new ADKBaseAgent.AgentRequest(
                    jobId, archResponse.getResult(), "application/json"
            );
            ADKBaseAgent.AgentResponse audResponse = executeAgentWithRetry(auditor, audRequest);
            if (!audResponse.isSuccess()) {
                return failedResponse(jobId, "Auditor failed", startTime);
            }
            jobState.put(jobId + "_auditor", "COMPLETED");

            // Record lineage
            String datasetVersion = "v" + (System.currentTimeMillis() / 1000);
            String mappingVersion = "m" + (System.currentTimeMillis() / 1000);
            recordLineage(jobId, datasetVersion, mappingVersion);

            long duration = System.currentTimeMillis() - startTime;
            log.info("✓ Conductor: ETL pipeline completed successfully in {}ms", duration);

            String finalReport = buildFinalReport(jobId, datasetVersion, mappingVersion);

            return new ADKBaseAgent.AgentResponse(jobId, finalReport, duration, true, null);

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("Conductor: Pipeline failed", e);
            return failedResponse(jobId, e.getMessage(), startTime);
        }
    }

    @Override
    public void shutdown() throws Exception {
        log.info("Shutting down Conductor Agent");
        scout.shutdown();
        cartographer.shutdown();
        navigator.shutdown();
        alchemist.shutdown();
        architect.shutdown();
        auditor.shutdown();
        vertexAI.shutdown();
    }

    /**
     * Execute agent with retry logic
     */
    private ADKBaseAgent.AgentResponse executeAgentWithRetry(ADKBaseAgent agent, ADKBaseAgent.AgentRequest request) throws Exception {
        int maxRetries = 3;
        int attempt = 1;

        while (attempt <= maxRetries) {
            try {
                return agent.execute(request);
            } catch (Exception e) {
                if (attempt == maxRetries) {
                    throw e;
                }
                long backoff = (long) Math.pow(2, attempt - 1) * 1000;
                log.warn("Agent {} failed (attempt {}/{}), retrying in {}ms",
                        agent.getFormalName(), attempt, maxRetries, backoff);
                Thread.sleep(backoff);
                attempt++;
            }
        }
        throw new Exception("Max retries exceeded");
    }

    /**
     * Build failed response
     */
    private ADKBaseAgent.AgentResponse failedResponse(String jobId, String error, long startTime) {
        long duration = System.currentTimeMillis() - startTime;
        log.error("Conductor: Pipeline failed with error: {}", error);
        return new ADKBaseAgent.AgentResponse(jobId, null, duration, false, error);
    }

    /**
     * Record lineage to BigQuery
     */
    private void recordLineage(String jobId, String datasetVersion, String mappingVersion) {
        try {
            log.info("Conductor: Recording lineage - jobId: {}, datasetVersion: {}, mappingVersion: {}",
                    jobId, datasetVersion, mappingVersion);
            // TODO: Write to job_lineage table
        } catch (Exception e) {
            log.warn("Conductor: Failed to record lineage", e);
        }
    }

    /**
     * Build final execution report
     */
    private String buildFinalReport(String jobId, String datasetVersion, String mappingVersion) {
        StringBuilder report = new StringBuilder();
        report.append("{\n");
        report.append("  \"jobId\": \"").append(jobId).append("\",\n");
        report.append("  \"status\": \"COMPLETED\",\n");
        report.append("  \"datasetVersion\": \"").append(datasetVersion).append("\",\n");
        report.append("  \"mappingVersion\": \"").append(mappingVersion).append("\",\n");
        report.append("  \"agentStatuses\": {\n");
        report.append("    \"scout\": \"").append(jobState.getOrDefault(jobId + "_scout", "PENDING")).append("\",\n");
        report.append("    \"cartographer\": \"").append(jobState.getOrDefault(jobId + "_cartographer", "PENDING")).append("\",\n");
        report.append("    \"navigator\": \"").append(jobState.getOrDefault(jobId + "_navigator", "PENDING")).append("\",\n");
        report.append("    \"alchemist\": \"").append(jobState.getOrDefault(jobId + "_alchemist", "PENDING")).append("\",\n");
        report.append("    \"architect\": \"").append(jobState.getOrDefault(jobId + "_architect", "PENDING")).append("\",\n");
        report.append("    \"auditor\": \"").append(jobState.getOrDefault(jobId + "_auditor", "PENDING")).append("\"\n");
        report.append("  }\n");
        report.append("}");
        return report.toString();
    }
}

