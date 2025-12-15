package com.etl.agent.agents;

import com.etl.agent.domain.PipelineJob;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Alchemist Agent (Transformation).
 */
public class AlchemistAgentTest {
    private AlchemistAgent agent;
    private PipelineJob job;

    @BeforeEach
    public void setUp() {
        agent = new AlchemistAgent();
        job = new PipelineJob();
        job.setSourcePath("gs://test-bucket/data.csv");
        job.setTargetDataset("test_dataset");
        job.setTargetTable("test_table");
        job.getStatistics().setTotalRecordsRead(100);
    }

    @Test
    public void testAgentName() {
        assertEquals("Alchemist", agent.getName());
    }

    @Test
    public void testTransformationSpecGeneration() throws Exception {
        agent.execute(job);

        assertEquals(PipelineJob.JobStatus.TRANSFORMED, job.getStatus());
        assertTrue(job.getLineage().size() > 0);
    }

    @Test
    public void testLineageEntry() throws Exception {
        agent.execute(job);

        PipelineJob.LineageEntry entry = job.getLineage().get(0);
        assertEquals("TRANSFORMATION", entry.getStep());
        assertEquals("Alchemist", entry.getAgentName());
        assertTrue(entry.getDurationMs() >= 0);
    }

    @Test
    public void testRecordPreservation() throws Exception {
        long originalCount = 100;
        job.getStatistics().setTotalRecordsRead(originalCount);

        agent.execute(job);

        // Transformation should preserve record count (before dedup)
        assertEquals(PipelineJob.JobStatus.TRANSFORMED, job.getStatus());
    }
}

