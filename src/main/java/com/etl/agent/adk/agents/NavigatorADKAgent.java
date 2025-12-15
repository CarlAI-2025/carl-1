package com.etl.agent.adk.agents;

import com.etl.agent.adk.core.ADKBaseAgent;
import com.etl.agent.adk.core.VertexAIClientCore;
import lombok.extern.slf4j.Slf4j;

/**
 * Navigator Agent - Standards Mapping & Canonical Model
 * Formal name: Navigator
 * Triggered by: Conductor
 *
 * Responsibilities (from design.txt):
 * 1. Maps source fields to canonical security master model
 * 2. Resolves conflicts
 * 3. Recommends identifier enrichment (FIGI) and standard codes (MIC, CFI)
 * 4. Generates Mapping Spec YAML/JSON + rationale
 * 5. Outputs confidence score per mapping
 */
@Slf4j
public class NavigatorADKAgent implements ADKBaseAgent {

    private final VertexAIClientCore vertexAI;

    public NavigatorADKAgent(String projectId, String location) {
        this.vertexAI = new VertexAIClientCore(projectId, location, "gemini-pro");
    }

    @Override
    public String getFormalName() {
        return "Navigator";
    }

    @Override
    public String getDescription() {
        return "Standards Mapping Agent: Maps fields to canonical model, recommends standards (FIGI, ISIN, MIC, CFI), outputs mapping spec with confidence scores";
    }

    @Override
    public void initialize() throws Exception {
        log.info("Initializing Navigator Agent");
        vertexAI.initialize();
    }

    @Override
    public AgentResponse execute(AgentRequest request) throws Exception {
        long startTime = System.currentTimeMillis();
        log.info("Navigator: Mapping source fields to canonical model");

        try {
            // Use Vertex AI to generate field mappings
            String mappingPrompt = String.format(
                    "As a data standards expert, map these source fields to the canonical security master model:\n" +
                    "Output JSON format:\n" +
                    "{\n" +
                    "  \"mappings\": [\n" +
                    "    {\n" +
                    "      \"sourceField\": \"name\",\n" +
                    "      \"targetField\": \"security_name\",\n" +
                    "      \"confidence\": 0.98,\n" +
                    "      \"standardCode\": \"FIGI\",\n" +
                    "      \"rationale\": \"Direct mapping of security name\"\n" +
                    "    }\n" +
                    "  ]\n" +
                    "}\n\n" +
                    "Schema:\n%s",
                    request.getPayload()
            );

            String mappingSpec = vertexAI.generateContent(mappingPrompt);

            long duration = System.currentTimeMillis() - startTime;
            log.info("Navigator: Field mapping completed in {}ms", duration);

            return new AgentResponse(request.getJobId(), mappingSpec, duration, true, null);

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("Navigator: Error generating mappings", e);
            return new AgentResponse(request.getJobId(), null, duration, false, e.getMessage());
        }
    }

    @Override
    public void shutdown() throws Exception {
        log.info("Shutting down Navigator Agent");
        vertexAI.shutdown();
    }
}

