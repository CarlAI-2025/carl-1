package com.etl.agent.agents;

import com.etl.agent.domain.*;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * Conductor Agent: Orchestrator/Root Agent.
 * Responsibilities:
 * - Owns end-to-end workflow
 * - Routes tasks
 * - Maintains job state
 * - Handles retries/failures
 * - Enforces ordering
 * - Decides when to request human approval
 * - Maintains run/version lineage
 */
@Slf4j
public class ConductorAgent {

    private final List<ETLAgent> agents;
    private final int maxRetries = 3;
    private final long retryDelayMs = 1000;

    public ConductorAgent() {
        this.agents = new ArrayList<>();
        // Initialize agents in execution order
        this.agents.add(new ScoutAgent());        // Ingestion
        this.agents.add(new CartographerAgent()); // Schema inference
        this.agents.add(new NavigatorAgent());    // Field mapping
        this.agents.add(new AlchemistAgent());    // Transformation
        this.agents.add(new ArchitectAgent());    // Pipeline generation
        this.agents.add(new AuditorAgent());      // Audit
    }

    /**
     * Execute the end-to-end ETL pipeline.
     */
    public void executePipeline(PipelineJob job) throws Exception {
        log.info("Conductor: Starting ETL pipeline for job {}", job.getJobId());
        job.setStartedAt(System.currentTimeMillis());

        try {
            for (ETLAgent agent : agents) {
                log.info("Conductor: Routing to {} agent", agent.getName());
                executeAgentWithRetry(agent, job);

                // Check for failures
                if (job.getStatus() == PipelineJob.JobStatus.FAILED) {
                    log.error("Conductor: Pipeline failed at {} stage", agent.getName());
                    break;
                }

                // Could add approval gates here for critical stages
                if (shouldRequestApproval(agent, job)) {
                    log.info("Conductor: Requesting human approval for {} results", agent.getName());
                    // In production, would wait for approval via message queue
                }
            }

            // Final status
            if (job.getStatus() != PipelineJob.JobStatus.FAILED) {
                job.setStatus(PipelineJob.JobStatus.COMPLETED);
                job.setCompletedAt(System.currentTimeMillis());
                log.info("Conductor: Pipeline completed successfully");
            }

        } catch (Exception e) {
            log.error("Conductor: Critical error - rolling back", e);
            job.setStatus(PipelineJob.JobStatus.ROLLED_BACK);
            job.setCompletedAt(System.currentTimeMillis());
            throw e;
        }
    }

    private void executeAgentWithRetry(ETLAgent agent, PipelineJob job) throws Exception {
        int attempt = 0;
        Exception lastException = null;

        while (attempt < maxRetries) {
            try {
                agent.execute(job);
                log.info("Conductor: {} agent completed successfully", agent.getName());
                return;
            } catch (Exception e) {
                attempt++;
                lastException = e;
                log.warn("Conductor: {} agent failed (attempt {}/{}): {}",
                        agent.getName(), attempt, maxRetries, e.getMessage());

                if (attempt < maxRetries) {
                    Thread.sleep(retryDelayMs * attempt); // Exponential backoff
                }
            }
        }

        // All retries exhausted
        log.error("Conductor: {} agent failed after {} attempts", agent.getName(), maxRetries);
        throw new Exception("Agent " + agent.getName() + " failed after " + maxRetries + " attempts", lastException);
    }

    private boolean shouldRequestApproval(ETLAgent agent, PipelineJob job) {
        // Request approval for high-risk stages or when quality score is low
        if (agent.getName().equals("Auditor")) {
            // Check if compliance score is below threshold
            return false; // Would check actual score in production
        }
        return false;
    }

    /**
     * Get job status and lineage.
     */
    public PipelineJob getJobStatus(String jobId) {
        // In production, would fetch from persistent store
        log.info("Conductor: Retrieving status for job {}", jobId);
        return null;
    }

    /**
     * Retrieve execution history for lineage tracking.
     */
    public List<PipelineJob.LineageEntry> getLineage(String jobId) {
        // In production, would fetch from persistent store
        log.info("Conductor: Retrieving lineage for job {}", jobId);
        return new ArrayList<>();
    }
}

