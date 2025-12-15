package com.etl.agent.adk.agents;

import com.etl.agent.adk.core.ADKBaseAgent;
import com.etl.agent.adk.core.VertexAIClientCore;
import lombok.extern.slf4j.Slf4j;

/**
 * Cartographer Agent - Schema Inference
 * Formal name: Cartographer
 * Triggered by: Conductor
 *
 * Responsibilities (from design.txt):
 * 1. Infers schema/semantics with confidence scores (Samples data)
 * 2. Detects schema drift vs prior version
 * 3. Outputs "Source Schema Contract" JSON
 */
@Slf4j
public class CartographerADKAgent implements ADKBaseAgent {

    private final VertexAIClientCore vertexAI;

    public CartographerADKAgent(String projectId, String location) {
        this.vertexAI = new VertexAIClientCore(projectId, location, "gemini-pro");
    }

    @Override
    public String getFormalName() {
        return "Cartographer";
    }

    @Override
    public String getDescription() {
        return "Schema Inference Agent: Infers schema with confidence scores, detects schema drift, outputs Source Schema Contract";
    }

    @Override
    public void initialize() throws Exception {
        log.info("Initializing Cartographer Agent");
        vertexAI.initialize();
    }

    @Override
    public AgentResponse execute(AgentRequest request) throws Exception {
        long startTime = System.currentTimeMillis();
        log.info("Cartographer: Inferring schema from file analysis");

        try {
            // Use Vertex AI to infer schema
            String schemaPrompt = String.format(
                    "Based on the file analysis provided, infer the data schema and output as JSON:\n" +
                    "{\n" +
                    "  \"fields\": [\n" +
                    "    {\"name\": \"field_name\", \"type\": \"data_type\", \"confidence\": 0.95}\n" +
                    "  ],\n" +
                    "  \"datasetVersion\": \"v1_TIMESTAMP\",\n" +
                    "  \"schemaFingerprint\": \"hash\"\n" +
                    "}\n\n" +
                    "File Analysis:\n%s",
                    request.getPayload()
            );

            String schemaContract = vertexAI.generateContent(schemaPrompt);

            long duration = System.currentTimeMillis() - startTime;
            log.info("Cartographer: Schema inference completed in {}ms", duration);

            return new AgentResponse(request.getJobId(), schemaContract, duration, true, null);

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("Cartographer: Error inferring schema", e);
            return new AgentResponse(request.getJobId(), null, duration, false, e.getMessage());
        }
    }

    @Override
    public void shutdown() throws Exception {
        log.info("Shutting down Cartographer Agent");
        vertexAI.shutdown();
    }
}

