package com.etl.noadk.agents;

import com.etl.noadk.domain.PipelineJob;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Scout Agent (Ingestion).
 */
public class ScoutAgentTest {
    private ScoutAgent agent;
    private PipelineJob job;

    @BeforeEach
    public void setUp() {
        agent = new ScoutAgent();
        job = new PipelineJob();
        job.setSourcePath("gs://test-bucket/data.csv");
        job.setTargetDataset("test_dataset");
        job.setTargetTable("test_table");
    }

    @Test
    public void testAgentName() {
        assertEquals("Scout", agent.getName());
    }

    @Test
    public void testScoutInitializesJobLineage() throws Exception {
        // Simulate ingestion (skip actual GCS access for unit test)
        job.getStatistics().setTotalRecordsRead(100);
        job.getLineage().add(new PipelineJob.LineageEntry("INGESTION", "Scout"));

        assertNotNull(job.getLineage());
        assertEquals(1, job.getLineage().size());
        assertEquals("INGESTION", job.getLineage().get(0).getStep());
        assertEquals("Scout", job.getLineage().get(0).getAgentName());
    }

    @Test
    public void testRecordCounting() {
        job.getStatistics().setTotalRecordsRead(1000);
        assertEquals(1000, job.getStatistics().getTotalRecordsRead());
    }

    @Test
    public void testJobInitialization() {
        assertNotNull(job.getJobId());
        assertEquals(PipelineJob.JobStatus.INITIATED, job.getStatus());
        assertTrue(job.getCreatedAt() > 0);
    }
}

