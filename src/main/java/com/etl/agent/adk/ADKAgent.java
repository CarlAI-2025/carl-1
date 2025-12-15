package com.etl.agent.adk;

import com.google.cloud.vertexai.api.Content;
import com.google.cloud.vertexai.api.GenerateContentRequest;
import com.google.cloud.vertexai.api.GenerateContentResponse;

/**
 * ADK-based agent interface for Google Cloud Vertex AI integration.
 * Agents use Google's Agent Development Kit for intelligent decision-making.
 */
public interface ADKAgent {

    /**
     * Initialize the agent with Vertex AI connection.
     */
    void initialize() throws Exception;

    /**
     * Execute agent task using Vertex AI's generative AI capabilities.
     *
     * @param request The content request for the agent
     * @return The response from Vertex AI
     */
    GenerateContentResponse executeWithAI(GenerateContentRequest request) throws Exception;

    /**
     * Get agent name for identification.
     */
    String getName();

    /**
     * Get agent capabilities/description.
     */
    String getCapabilities();

    /**
     * Cleanup resources.
     */
    void shutdown() throws Exception;
}

