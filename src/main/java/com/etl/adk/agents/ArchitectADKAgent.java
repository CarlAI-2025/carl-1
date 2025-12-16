package com.etl.adk.agents;

import com.etl.agent.adk.core.ADKBaseAgent;
import com.etl.agent.adk.core.VertexAIClientCore;
import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.BigQueryOptions;
import lombok.extern.slf4j.Slf4j;

/**
 * Architect Agent - Pipeline Generator
 * Formal name: Architect
 * Triggered by: Conductor
 *
 * Responsibilities (from design.txt):
 * 1. Generates runnable pipeline artifacts from mapping + transformation plan
 * 2. MVP: BigQuery SQL scripts + scheduled execution
 * 3. Scale (optional): Beam/Dataflow pipeline skeleton
 */
@Slf4j
public class ArchitectADKAgent implements ADKBaseAgent {

    private final VertexAIClientCore vertexAI;
    private final BigQuery bigQuery;

    public ArchitectADKAgent(String projectId, String location) {
        this.vertexAI = new VertexAIClientCore(projectId, location, "gemini-pro");
        this.bigQuery = BigQueryOptions.getDefaultInstance().getService();
    }

    @Override
    public String getFormalName() {
        return "Architect";
    }

    @Override
    public String getDescription() {
        return "Pipeline Generator: Generates BigQuery SQL scripts and Dataflow pipeline skeletons from mapping and transformation plans";
    }

    @Override
    public void initialize() throws Exception {
        log.info("Initializing Architect Agent");
        vertexAI.initialize();
    }

    @Override
    public AgentResponse execute(AgentRequest request) throws Exception {
        long startTime = System.currentTimeMillis();
        log.info("Architect: Generating pipeline artifacts");

        try {
            // Use Vertex AI to generate SQL
            String sqlPrompt = String.format(
                    "Generate a BigQuery SQL script that:\n" +
                    "1. Creates a staging table for the source data\n" +
                    "2. Applies transformation rules\n" +
                    "3. Handles deduplication\n" +
                    "4. Loads to target table with job tracking\n" +
                    "5. Records lineage with dataset_version and mapping_version\n\n" +
                    "Output format: SQL script with comments\n\n" +
                    "Transformation Rules:\n%s",
                    request.getPayload()
            );

            String sqlScript = vertexAI.generateContent(sqlPrompt);

            long duration = System.currentTimeMillis() - startTime;
            log.info("Architect: SQL generation completed in {}ms", duration);

            return new AgentResponse(request.getJobId(), sqlScript, duration, true, null);

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("Architect: Error generating pipeline", e);
            return new AgentResponse(request.getJobId(), null, duration, false, e.getMessage());
        }
    }

    @Override
    public void shutdown() throws Exception {
        log.info("Shutting down Architect Agent");
        vertexAI.shutdown();
    }
}

