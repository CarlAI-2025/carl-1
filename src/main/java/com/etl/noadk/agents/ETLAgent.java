package com.etl.noadk.agents;

import com.etl.noadk.domain.PipelineJob;

/**
 * Base interface for all ETL agents.
 */
public interface ETLAgent {
    /**
     * Gets the agent's name.
     */
    String getName();

    /**
     * Executes the agent's responsibilities.
     */
    void execute(PipelineJob job) throws Exception;
}

