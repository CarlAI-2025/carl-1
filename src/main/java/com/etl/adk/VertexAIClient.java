package com.etl.adk;

import com.google.cloud.vertexai.VertexAI;
import com.google.cloud.vertexai.generativeai.GenerativeModel;
import com.google.cloud.vertexai.api.GenerateContentRequest;
import com.google.cloud.vertexai.api.GenerateContentResponse;
import com.google.cloud.vertexai.api.Content;
import com.google.cloud.vertexai.api.Part;
import lombok.extern.slf4j.Slf4j;

/**
 * Vertex AI client for ADK-based agents.
 * Provides Generative AI capabilities using Google's Agent Development Kit.
 */
@Slf4j
public class VertexAIClient {

    private final String projectId;
    private final String location;
    private final String modelName;
    private VertexAI vertexAI;
    private GenerativeModel model;

    public VertexAIClient(String projectId, String location, String modelName) {
        this.projectId = projectId;
        this.location = location;
        this.modelName = modelName;
    }

    /**
     * Initialize Vertex AI client.
     */
    public void initialize() throws Exception {
        log.info("Initializing Vertex AI client for project: {}", projectId);

        try {
            this.vertexAI = new VertexAI(projectId, location);
            this.model = new GenerativeModel(modelName, vertexAI);
            log.info("Vertex AI initialized successfully with model: {}", modelName);
        } catch (Exception e) {
            log.error("Failed to initialize Vertex AI", e);
            throw e;
        }
    }

    /**
     * Generate content using Vertex AI's Generative AI model.
     * Used by ADK agents for intelligent decision-making.
     */
    public GenerateContentResponse generateContent(String prompt) throws Exception {
        if (model == null) {
            throw new IllegalStateException("VertexAI client not initialized. Call initialize() first.");
        }

        log.debug("Generating content with prompt: {}", prompt.substring(0, Math.min(100, prompt.length())));

        try {
            // Create content request using ADK
            Content.Builder contentBuilder = Content.newBuilder();
            contentBuilder.addParts(Part.newBuilder()
                    .setText(prompt)
                    .build());

            GenerateContentRequest request = GenerateContentRequest.newBuilder()
                    .addContents(contentBuilder.build())
                    .build();

            return model.generateContent(request.getContentsList());
        } catch (Exception e) {
            log.error("Error generating content from Vertex AI", e);
            throw e;
        }
    }

    /**
     * Stream content generation for real-time responses.
     * Useful for long-running agent tasks.
     */
    public void streamContent(String prompt, StreamCallback callback) throws Exception {
        if (model == null) {
            throw new IllegalStateException("VertexAI client not initialized");
        }

        log.info("Streaming content generation");

        try {
            Content.Builder contentBuilder = Content.newBuilder();
            contentBuilder.addParts(Part.newBuilder()
                    .setText(prompt)
                    .build());

            // For streaming, we would use the streaming API
            // This is a simplified version
            model.generateContentStream(contentBuilder.build())
                    .forEach(response -> {
                        try {
                            callback.onContent(response);
                        } catch (Exception e) {
                            log.error("Error in stream callback", e);
                        }
                    });
        } catch (Exception e) {
            log.error("Error streaming content", e);
            throw e;
        }
    }

    /**
     * Get model information.
     */
    public String getModelName() {
        return modelName;
    }

    /**
     * Shutdown Vertex AI client.
     */
    public void shutdown() throws Exception {
        log.info("Shutting down Vertex AI client");
        if (vertexAI != null) {
            vertexAI.close();
        }
    }

    /**
     * Callback interface for streaming content.
     */
    @FunctionalInterface
    public interface StreamCallback {
        void onContent(GenerateContentResponse response) throws Exception;
    }
}

