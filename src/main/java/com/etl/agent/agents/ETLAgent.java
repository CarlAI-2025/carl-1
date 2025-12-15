package com.etl.agent.agents;

import com.etl.agent.domain.*;
import java.util.List;
import java.util.Map;

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

