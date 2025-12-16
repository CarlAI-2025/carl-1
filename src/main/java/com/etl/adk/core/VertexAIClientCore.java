package com.etl.adk.core;

import com.google.cloud.vertexai.VertexAI;
import com.google.cloud.vertexai.generativeai.GenerativeModel;
import com.google.cloud.vertexai.api.*;
import lombok.extern.slf4j.Slf4j;

/**
 * Unified Vertex AI client for all ADK agents.
 * Provides Gemini Pro access for intelligent agent reasoning.
 */
@Slf4j
public class VertexAIClientCore {

    private final String projectId;
    private final String location;
    private final String modelName;
    private VertexAI vertexAI;
    private GenerativeModel model;

    public VertexAIClientCore(String projectId, String location, String modelName) {
        this.projectId = projectId;
        this.location = location;
        this.modelName = modelName;
    }

    /**
     * Initialize Vertex AI client
     */
    public void initialize() throws Exception {
        log.info("Initializing Vertex AI client for project: {}, location: {}, model: {}",
                projectId, location, modelName);

        try {
            this.vertexAI = new VertexAI(projectId, location);
            this.model = new GenerativeModel(modelName, vertexAI);
            log.info("✓ Vertex AI initialized successfully");
        } catch (Exception e) {
            log.error("Failed to initialize Vertex AI", e);
            throw e;
        }
    }

    /**
     * Generate content using Vertex AI
     */
    public String generateContent(String prompt) throws Exception {
        if (model == null) {
            throw new IllegalStateException("VertexAI client not initialized");
        }

        log.debug("Generating content with prompt length: {}", prompt.length());

        try {
            Content.Builder contentBuilder = Content.newBuilder();
            contentBuilder.addParts(Part.newBuilder()
                    .setText(prompt)
                    .build());

            GenerateContentRequest request = GenerateContentRequest.newBuilder()
                    .addContents(contentBuilder.build())
                    .build();

            GenerateContentResponse response = model.generateContent(request.getContentsList());
            return extractTextFromResponse(response);
        } catch (Exception e) {
            log.error("Error generating content", e);
            throw e;
        }
    }

    /**
     * Extract text from Vertex AI response
     */
    private String extractTextFromResponse(GenerateContentResponse response) {
        try {
            if (!response.getCandidatesList().isEmpty()) {
                var candidate = response.getCandidates(0);
                if (candidate.hasContent()) {
                    var content = candidate.getContent();
                    if (content.getPartsCount() > 0) {
                        return content.getParts(0).getText();
                    }
                }
            }
            return "";
        } catch (Exception e) {
            log.error("Error extracting response text", e);
            return "";
        }
    }

    /**
     * Shutdown Vertex AI client
     */
    public void shutdown() throws Exception {
        log.info("Shutting down Vertex AI client");
        if (vertexAI != null) {
            vertexAI.close();
        }
    }
}

