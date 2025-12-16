package com.etl.adk.agents;

import com.etl.agent.adk.core.ADKBaseAgent;
import com.etl.agent.adk.core.VertexAIClientCore;
import lombok.extern.slf4j.Slf4j;

/**
 * Alchemist Agent - Transformation & Enrichment
 * Formal name: Alchemist
 * Triggered by: Conductor
 *
 * Responsibilities (from design.txt):
 * 1. Produces cleaning + enrichment logic: normalization (case/trim), currency/date parsing, dedup + survivorship,
 *    missing value handling, identifier validation, joins to lookups
 * 2. Outputs Transformation Rules (declarative)
 * 3. Generates Executable Templates
 */
@Slf4j
public class AlchemistADKAgent implements ADKBaseAgent {

    private final VertexAIClientCore vertexAI;

    public AlchemistADKAgent(String projectId, String location) {
        this.vertexAI = new VertexAIClientCore(projectId, location, "gemini-pro");
    }

    @Override
    public String getFormalName() {
        return "Alchemist";
    }

    @Override
    public String getDescription() {
        return "Transformation Agent: Produces cleaning rules, enrichment logic, deduplication rules, outputs declarative transformation specs";
    }

    @Override
    public void initialize() throws Exception {
        log.info("Initializing Alchemist Agent");
        vertexAI.initialize();
    }

    @Override
    public AgentResponse execute(AgentRequest request) throws Exception {
        long startTime = System.currentTimeMillis();
        log.info("Alchemist: Generating transformation rules");

        try {
            // Use Vertex AI to generate transformation rules
            String transformPrompt = String.format(
                    "Generate declarative transformation rules in JSON format:\n" +
                    "{\n" +
                    "  \"cleaningRules\": [\n" +
                    "    {\"field\": \"name\", \"operations\": [\"TRIM\", \"UPPERCASE\"]}\n" +
                    "  ],\n" +
                    "  \"deduplicationRules\": {\n" +
                    "    \"keyFields\": [\"id\"],\n" +
                    "    \"survivorshipStrategy\": \"KEEP_LATEST\"\n" +
                    "  },\n" +
                    "  \"enrichmentRules\": [\n" +
                    "    {\"sourceField\": \"code\", \"lookupTable\": \"standards\", \"targetField\": \"description\"}\n" +
                    "  ]\n" +
                    "}\n\n" +
                    "Field Mapping:\n%s",
                    request.getPayload()
            );

            String transformRules = vertexAI.generateContent(transformPrompt);

            long duration = System.currentTimeMillis() - startTime;
            log.info("Alchemist: Transformation rules generated in {}ms", duration);

            return new AgentResponse(request.getJobId(), transformRules, duration, true, null);

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("Alchemist: Error generating transformation rules", e);
            return new AgentResponse(request.getJobId(), null, duration, false, e.getMessage());
        }
    }

    @Override
    public void shutdown() throws Exception {
        log.info("Shutting down Alchemist Agent");
        vertexAI.shutdown();
    }
}

