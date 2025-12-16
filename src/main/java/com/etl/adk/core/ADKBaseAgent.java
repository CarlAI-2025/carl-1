package com.etl.adk.core;

import com.google.cloud.vertexai.api.GenerateContentResponse;
import lombok.Getter;

/**
 * Base ADK Agent interface conforming to design.txt specifications.
 * All agents in the pipeline extend this interface.
 */
public interface ADKBaseAgent {

    /**
     * Get the formal agent name as specified in design.txt
     */
    String getFormalName();

    /**
     * Get agent description and responsibilities
     */
    String getDescription();

    /**
     * Initialize the agent with necessary resources
     */
    void initialize() throws Exception;

    /**
     * Execute agent task and return response
     */
    AgentResponse execute(AgentRequest request) throws Exception;

    /**
     * Shutdown and cleanup resources
     */
    void shutdown() throws Exception;

    /**
     * Agent request wrapper
     */
    @Getter
    class AgentRequest {
        private final String jobId;
        private final String payload;
        private final String contentType;

        public AgentRequest(String jobId, String payload, String contentType) {
            this.jobId = jobId;
            this.payload = payload;
            this.contentType = contentType;
        }
    }

    /**
     * Agent response wrapper
     */
    @Getter
    class AgentResponse {
        private final String jobId;
        private final String result;
        private final long processingTimeMs;
        private final boolean success;
        private final String errorMessage;

        public AgentResponse(String jobId, String result, long processingTimeMs,
                            boolean success, String errorMessage) {
            this.jobId = jobId;
            this.result = result;
            this.processingTimeMs = processingTimeMs;
            this.success = success;
            this.errorMessage = errorMessage;
        }
    }
}

