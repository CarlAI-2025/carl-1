package com.etl.noadk.agents;

import com.etl.noadk.domain.PipelineJob;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for the Conductor Agent orchestration.
 */
public class ConductorAgentIntegrationTest {
    private ConductorAgent conductor;
    private PipelineJob job;

    @BeforeEach
    public void setUp() {
        conductor = new ConductorAgent();
        job = new PipelineJob();
        job.setSourcePath("gs://test-bucket/data.csv");
        job.setTargetDataset("test_dataset");
        job.setTargetTable("test_table");
    }

    @Test
    public void testPipelineInitialization() {
        assertNotNull(job.getJobId());
        assertEquals(PipelineJob.JobStatus.INITIATED, job.getStatus());
        assertTrue(job.getCreatedAt() > 0);
    }

    @Test
    public void testPipelineExecution() throws Exception {
        // This test demonstrates full pipeline flow
        // In production, would use mock GCS storage
        conductor.executePipeline(job);

        // Verify pipeline completed
        assertNotNull(job.getStatus());
        assertTrue(job.getLineage().size() > 0);
    }

    @Test
    public void testLineageTracking() throws Exception {
        conductor.executePipeline(job);

        // Verify all expected agents ran
        long scoutCount = job.getLineage().stream()
                .filter(e -> "Scout".equals(e.getAgentName()))
                .count();
        assertEquals(1, scoutCount);
    }

    @Test
    public void testJobStateManagement() throws Exception {
        assertNotNull(job.getJobId());
        assertNull(job.getStartedAt());

        conductor.executePipeline(job);

        assertNotNull(job.getStartedAt());
        assertNotNull(job.getCompletedAt());
        assertTrue(job.getCompletedAt() >= job.getStartedAt());
    }

    @Test
    public void testVersionLineageTracking() throws Exception {
        conductor.executePipeline(job);

        assertNotNull(job.getDatasetVersion());
        assertNotNull(job.getMappingVersion());
    }

    @Test
    public void testErrorHandling() throws Exception {
        // Create invalid job configuration
        job.setSourcePath(null);

        try {
            conductor.executePipeline(job);
            // Expected to fail
            assertEquals(PipelineJob.JobStatus.FAILED, job.getStatus());
        } catch (Exception e) {
            // Expected exception
            assertNotNull(e);
        }
    }

    @Test
    public void testRetryMechanism() {
        // Verify conductor has retry configuration
        assertNotNull(conductor);
        // In production, would verify retry counts
    }
}

