package com.etl.agent.agents;

import com.etl.agent.domain.PipelineJob;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Cartographer Agent (Schema Inference).
 */
public class CartographerAgentTest {
    private CartographerAgent agent;
    private PipelineJob job;

    @BeforeEach
    public void setUp() {
        agent = new CartographerAgent();
        job = new PipelineJob();
        job.setSourcePath("gs://test-bucket/data.csv");
        job.setTargetDataset("test_dataset");
        job.setTargetTable("test_table");
        job.getStatistics().setTotalRecordsRead(100);
    }

    @Test
    public void testAgentName() {
        assertEquals("Cartographer", agent.getName());
    }

    @Test
    public void testSchemaDiscovery() throws Exception {
        agent.execute(job);

        assertEquals(PipelineJob.JobStatus.SCHEMA_DISCOVERED, job.getStatus());
        assertNotNull(job.getDatasetVersion());
        assertTrue(job.getLineage().size() > 0);
    }

    @Test
    public void testLineageRecording() throws Exception {
        agent.execute(job);

        PipelineJob.LineageEntry entry = job.getLineage().get(0);
        assertEquals("SCHEMA_INFERENCE", entry.getStep());
        assertEquals("Cartographer", entry.getAgentName());
        assertTrue(entry.getDurationMs() >= 0);
    }

    @Test
    public void testVersionTracking() throws Exception {
        agent.execute(job);

        assertNotNull(job.getDatasetVersion());
        assertTrue(job.getDatasetVersion().startsWith("v1_"));
    }
}

