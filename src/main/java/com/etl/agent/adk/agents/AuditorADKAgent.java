package com.etl.agent.adk.agents;

import com.etl.agent.adk.core.ADKBaseAgent;
import com.etl.agent.adk.core.VertexAIClientCore;
import com.google.cloud.bigquery.*;
import lombok.extern.slf4j.Slf4j;

/**
 * Auditor Agent - Data Quality & Compliance
 * Formal name: Auditor
 * Triggered by: Conductor
 *
 * Responsibilities (from design.txt):
 * 1. DQ scorecard (table)
 * 2. Compliance score (0–100)
 * 3. Reasoning log (mapping & transformation rationale)
 */
@Slf4j
public class AuditorADKAgent implements ADKBaseAgent {

    private final VertexAIClientCore vertexAI;
    private final BigQuery bigQuery;

    public AuditorADKAgent(String projectId, String location) {
        this.vertexAI = new VertexAIClientCore(projectId, location, "gemini-pro");
        this.bigQuery = BigQueryOptions.getDefaultInstance().getService();
    }

    @Override
    public String getFormalName() {
        return "Auditor";
    }

    @Override
    public String getDescription() {
        return "Quality & Compliance Agent: Generates DQ scorecards, compliance scores, and reasoning logs for audit trail";
    }

    @Override
    public void initialize() throws Exception {
        log.info("Initializing Auditor Agent");
        vertexAI.initialize();
    }

    @Override
    public AgentResponse execute(AgentRequest request) throws Exception {
        long startTime = System.currentTimeMillis();
        log.info("Auditor: Analyzing data quality and compliance");

        try {
            // Use Vertex AI to generate quality report
            String qualityPrompt = String.format(
                    "Generate a comprehensive data quality report in JSON format:\n" +
                    "{\n" +
                    "  \"dqScore\": 95,\n" +
                    "  \"complianceScore\": 92,\n" +
                    "  \"metrics\": {\n" +
                    "    \"nullPercentage\": 0.5,\n" +
                    "    \"duplicatePercentage\": 0.2,\n" +
                    "    \"outlierPercentage\": 1.0\n" +
                    "  },\n" +
                    "  \"recommendations\": [],\n" +
                    "  \"reasoningLog\": \"Detailed explanation of quality assessment\"\n" +
                    "}\n\n" +
                    "Load Job Data:\n%s",
                    request.getPayload()
            );

            String qualityReport = vertexAI.generateContent(qualityPrompt);

            long duration = System.currentTimeMillis() - startTime;
            log.info("Auditor: Quality assessment completed in {}ms", duration);

            return new AgentResponse(request.getJobId(), qualityReport, duration, true, null);

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("Auditor: Error assessing quality", e);
            return new AgentResponse(request.getJobId(), null, duration, false, e.getMessage());
        }
    }

    @Override
    public void shutdown() throws Exception {
        log.info("Shutting down Auditor Agent");
        vertexAI.shutdown();
    }
}

